package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.entity.footbar.FootbarSyncState;
import com.jumbo.trus.repository.footbar.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FootbarSyncStateService {
    private final FootbarAccountRepository accounts;
    private final FootbarSyncStateRepository states;
    private final FootbarAutoSyncProperties properties;

    @Transactional
    public String claim(Long id, Instant now) {
        var account = accounts.findLockedById(id).orElse(null);
        if (account == null) return null;
        FootbarSyncState state = states.findById(id).orElseGet(() -> {
            FootbarSyncState created = new FootbarSyncState();
            created.setAccountId(id);
            return created;
        });
        if (state.getLeaseUntil() != null && state.getLeaseUntil().isAfter(now)) return null;
        if (account.getLinkedAt() != null && (state.getLastAttemptAt() == null
                || account.getLinkedAt().isAfter(state.getLastAttemptAt()))) {
            state.setWarning(null);
            state.setReconnectRequired(false);
            state.setNotifiedAt(null);
            state.setLastAttemptAt(null);
        }
        if (state.getLastAttemptAt() != null && state.getLastAttemptAt()
                .plus(properties.getRetryMinutes(), ChronoUnit.MINUTES).isAfter(now)) return null;
        state.setLastAttemptAt(now);
        state.setLeaseUntil(now.plus(30, ChronoUnit.MINUTES));
        state.setLeaseOwner(UUID.randomUUID().toString());
        states.save(state);
        return state.getLeaseOwner();
    }

    @Transactional
    public boolean renew(Long id, String owner, Instant now) {
        if (accounts.findLockedById(id).isEmpty()) return false;
        FootbarSyncState state = states.findById(id).orElseThrow();
        if (!owner.equals(state.getLeaseOwner())) return false;
        state.setLeaseUntil(now.plus(30, ChronoUnit.MINUTES));
        states.save(state);
        return true;
    }

    @Transactional
    public boolean finish(FootbarSyncPlanner.Work work, String owner, Instant now, String warning, boolean reconnect) {
        var account = accounts.findLockedById(work.accountId()).orElse(null);
        if (account == null) return false;
        FootbarSyncState state = states.findById(work.accountId()).orElseThrow();
        if (!owner.equals(state.getLeaseOwner())) return false;
        state.setLeaseUntil(null);
        state.setLeaseOwner(null);
        Instant currentConnection = account.getLinkedAt() == null ? Instant.EPOCH : account.getLinkedAt();
        if (!currentConnection.equals(work.connectionAt())) {
            states.save(state);
            return false; // A newer connection must get its own history import.
        }
        boolean notify = warning != null && (state.getNotifiedAt() == null
                || state.isReconnectRequired() != reconnect
                || (!reconnect && state.getNotifiedAt().plus(24, ChronoUnit.HOURS).isBefore(now)));
        state.setWarning(warning);
        state.setReconnectRequired(reconnect);
        if (warning == null) {
            state.setLastSuccessAt(now);
            if (work.history()) state.setImportedConnectionAt(work.connectionAt());
            state.setNotifiedAt(null);
        } else if (notify) state.setNotifiedAt(now);
        states.save(state);
        return notify;
    }
}
