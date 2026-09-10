package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.footbar.FootbarAccountRepository;
import com.jumbo.trus.service.activity.footbar.connect.FootbarReconnectRequiredException;
import com.jumbo.trus.service.activity.footbar.session.FootbarSessionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FootbarAutoSyncJob {
    public static final String RECONNECT = "Propojení s Footbarem už není platné. Otevři Footbar v aplikaci a znovu propoj svůj účet.";
    public static final String TEMPORARY = "Session z Footbaru se nepodařilo načíst. Zkus zkontrolovat propojení účtu. Synchronizaci zopakujeme automaticky.";
    private final FootbarSyncPlanner planner;
    private final FootbarSyncStateService states;
    private final FootbarAccountRepository accounts;
    private final AppTeamRepository teams;
    private final FootbarSessionProcessor processor;
    private final FootbarSyncNotifications notifications;
    private final FootbarAutoSyncProperties properties;
    private final org.springframework.core.task.TaskExecutor taskExecutor;
    private final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean();

    // Polling every minute discovers newly linked accounts promptly. Durable
    // lastAttemptAt gates actual imports to one per account every five minutes.
    @Scheduled(fixedDelayString = "${footbar.auto-sync.poll-ms:60000}", initialDelayString = "${footbar.auto-sync.initial-delay-ms:30000}")
    public void run() {
        if (!properties.isEnabled() || !running.compareAndSet(false, true)) return;
        try {
            taskExecutor.execute(() -> {
                try {
                    for (var work : planner.plan(Instant.now())) {
                        try { process(work); }
                        catch (RuntimeException e) { log.error("Footbar automatic job failed for account {}", work.accountId(), e); }
                    }
                } finally { running.set(false); }
            });
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    void process(FootbarSyncPlanner.Work work) {
        String owner = states.claim(work.accountId(), Instant.now());
        if (owner == null) return;
        String warning = null;
        boolean reconnect = false;
        var account = accounts.findById(work.accountId()).orElseThrow();
        for (Long teamId : work.teamIds()) {
            if (!states.renew(work.accountId(), owner, Instant.now())) return;
            try {
                processor.saveSessions(account, teams.findById(teamId).orElseThrow());
            } catch (FootbarReconnectRequiredException e) {
                warning = RECONNECT;
                reconnect = true;
                break;
            } catch (RuntimeException e) {
                warning = TEMPORARY;
                log.warn("Footbar automatic import failed: account={}, team={}", work.accountId(), teamId, e);
            }
        }
        if (states.finish(work, owner, Instant.now(), warning, reconnect)) {
            notifications.notifyOwner(work.userId(), warning);
        }
    }
}
