package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.footbar.FootbarAccountRepository;
import com.jumbo.trus.service.activity.footbar.connect.FootbarReconnectRequiredException;
import com.jumbo.trus.service.activity.footbar.session.FootbarSessionProcessor;
import org.junit.jupiter.api.*;
import org.springframework.core.task.SyncTaskExecutor;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FootbarAutoSyncJobTest {
    FootbarSyncPlanner planner = mock(FootbarSyncPlanner.class);
    FootbarSyncStateService states = mock(FootbarSyncStateService.class);
    FootbarAccountRepository accounts = mock(FootbarAccountRepository.class);
    AppTeamRepository teams = mock(AppTeamRepository.class);
    FootbarSessionProcessor processor = mock(FootbarSessionProcessor.class);
    FootbarSyncNotifications notifications = mock(FootbarSyncNotifications.class);
    FootbarAutoSyncProperties properties = new FootbarAutoSyncProperties();
    FootbarAutoSyncJob job = new FootbarAutoSyncJob(planner, states, accounts, teams, processor, notifications,
            properties, new SyncTaskExecutor());
    FootbarAccountEntity account = new FootbarAccountEntity();
    AppTeamEntity team = new AppTeamEntity();
    FootbarSyncPlanner.Work work = new FootbarSyncPlanner.Work(1L, 2L, Instant.EPOCH, Set.of(3L), true);

    @BeforeEach void setup() {
        account.setId(1L);
        when(states.claim(eq(1L), any())).thenReturn("lease");
        when(states.renew(eq(1L), eq("lease"), any())).thenReturn(true);
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        when(teams.findById(3L)).thenReturn(Optional.of(team));
    }

    @Test void successfulHistoryImportFinishesDurableWorkWithoutWarning() {
        job.process(work);
        verify(processor).saveSessions(account, team);
        verify(states).finish(eq(work), eq("lease"), any(), isNull(), eq(false));
        verifyNoInteractions(notifications);
    }

    @Test void invalidTokenNotifiesOnlyItsOwnerAndRetainsPendingHistory() {
        doThrow(new FootbarReconnectRequiredException()).when(processor).saveSessions(account, team);
        when(states.finish(eq(work), eq("lease"), any(), eq(FootbarAutoSyncJob.RECONNECT), eq(true))).thenReturn(true);
        job.process(work);
        verify(notifications).notifyOwner(2L, FootbarAutoSyncJob.RECONNECT);
    }

    @Test void unavailableLeaseSkipsNetworkCall() {
        when(states.claim(eq(1L), any())).thenReturn(null);
        job.process(work);
        verifyNoInteractions(processor, notifications);
    }

    @Test void failureDoesNotStopNextAccountAndNotificationIsNotRepeated() {
        var second = new FootbarSyncPlanner.Work(4L, 5L, Instant.EPOCH, Set.of(3L), true);
        var other = new FootbarAccountEntity();
        other.setId(4L);
        when(planner.plan(any())).thenReturn(List.of(work, second));
        when(states.claim(eq(4L), any())).thenReturn("other");
        when(states.renew(eq(4L), eq("other"), any())).thenReturn(true);
        when(accounts.findById(4L)).thenReturn(Optional.of(other));
        doThrow(new IllegalStateException("offline")).when(processor).saveSessions(account, team);
        job.run();
        verify(processor).saveSessions(other, team);
        verify(states).finish(eq(work), eq("lease"), any(), eq(FootbarAutoSyncJob.TEMPORARY), eq(false));
        verifyNoInteractions(notifications);
    }

    @Test void disabledJobDoesNotReadAccounts() {
        properties.setEnabled(false);
        job.run();
        verifyNoInteractions(planner, accounts, processor);
    }
}
