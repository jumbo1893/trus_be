package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.entity.footbar.*;
import com.jumbo.trus.repository.footbar.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FootbarSyncStateServiceTest {
    FootbarAccountRepository accounts = mock(FootbarAccountRepository.class);
    FootbarSyncStateRepository states = mock(FootbarSyncStateRepository.class);
    FootbarSyncStateService service = new FootbarSyncStateService(accounts, states, new FootbarAutoSyncProperties());
    Instant now = Instant.parse("2026-09-10T19:00:00Z");
    FootbarSyncState state = new FootbarSyncState();
    FootbarAccountEntity account = new FootbarAccountEntity();
    FootbarSyncPlanner.Work work;

    @BeforeEach void setup() {
        state.setAccountId(1L); account.setId(1L); account.setLinkedAt(now.minusSeconds(3600));
        work = new FootbarSyncPlanner.Work(1L, 2L, account.getLinkedAt(), Set.of(3L), true);
        when(accounts.findLockedById(1L)).thenReturn(Optional.of(account));
        when(states.findById(1L)).thenReturn(Optional.of(state));
    }

    @Test void leasePreventsConcurrentImportAndFiveMinuteGateSurvivesCompletion() {
        String owner = service.claim(1L, now);
        assertNotNull(owner);
        assertNull(service.claim(1L, now.plusSeconds(60)));
        service.finish(work, owner, now.plusSeconds(20), null, false);
        assertNull(service.claim(1L, now.plusSeconds(299)));
        assertNotNull(service.claim(1L, now.plusSeconds(300)));
        assertEquals(work.connectionAt(), state.getImportedConnectionAt());
    }

    @Test void warningIsSentOnceAndClearedOnSuccess() {
        String owner = service.claim(1L, now);
        assertTrue(service.finish(work, owner, now, "reconnect", true));
        owner = service.claim(1L, now.plusSeconds(300));
        assertFalse(service.finish(work, owner, now.plusSeconds(300), "reconnect", true));
        owner = service.claim(1L, now.plusSeconds(600));
        service.finish(work, owner, now.plusSeconds(600), null, false);
        assertNull(state.getWarning());
        assertNull(state.getNotifiedAt());
    }

    @Test void expiredLeaseCanBeReclaimedButOldWorkerCannotFinishNewLease() {
        String old = service.claim(1L, now);
        String newer = service.claim(1L, now.plusSeconds(1801));
        assertNotNull(newer);
        assertFalse(service.finish(work, old, now.plusSeconds(1802), "error", false));
        assertEquals(newer, state.getLeaseOwner());
    }

    @Test void reconnectDuringImportCannotBeMarkedImportedByOldWork() {
        String owner = service.claim(1L, now);
        account.setLinkedAt(now.plusSeconds(1));
        assertFalse(service.finish(work, owner, now.plusSeconds(2), "reconnect", true));
        assertNull(state.getImportedConnectionAt());
        assertNull(state.getWarning());
        assertNotNull(service.claim(1L, now.plusSeconds(3)));
    }
}
