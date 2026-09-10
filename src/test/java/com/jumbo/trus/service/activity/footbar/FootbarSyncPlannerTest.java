package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.*;
import com.jumbo.trus.entity.football.*;
import com.jumbo.trus.entity.footbar.*;
import com.jumbo.trus.entity.participation.*;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.footbar.*;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FootbarSyncPlannerTest {
    final Instant now = Instant.parse("2026-09-10T19:30:00Z");
    final FootbarAccountRepository accounts = mock(FootbarAccountRepository.class);
    final FootbarSyncStateRepository states = mock(FootbarSyncStateRepository.class);
    final FootbarSessionRepository sessions = mock(FootbarSessionRepository.class);
    final MatchRepository matches = mock(MatchRepository.class);
    final FootballMatchRepository fixtures = mock(FootballMatchRepository.class);
    final MatchParticipationRepository participation = mock(MatchParticipationRepository.class);
    final FootbarSyncPlanner planner = new FootbarSyncPlanner(accounts, states, sessions, matches,
            new FootbarAutoSyncProperties(), fixtures, participation);
    FootbarAccountEntity account;
    FootbarSyncState state;
    AppTeamEntity team;
    PlayerEntity player;
    MatchEntity match;

    @BeforeEach void setup() {
        team = new AppTeamEntity(); team.setId(1L);
        player = new PlayerEntity(); player.setId(7L);
        UserTeamRole role = new UserTeamRole(); role.setAppTeam(team); role.setPlayer(player);
        UserEntity user = new UserEntity(); user.setId(9L); user.setTeamRoles(List.of(role));
        account = new FootbarAccountEntity(); account.setId(2L); account.setUser(user);
        account.setLinkedAt(now.minusSeconds(86400)); account.setRefreshToken("token");
        state = new FootbarSyncState(); state.setAccountId(2L); state.setImportedConnectionAt(account.getLinkedAt());
        state.setLastSuccessAt(now.minusSeconds(86400));
        when(accounts.findAll()).thenReturn(List.of(account));
        when(states.findById(2L)).thenReturn(Optional.of(state));
        match = new MatchEntity(); match.setId(3L); match.setAppTeam(team);
        match.setDate(Date.from(now.minusSeconds(5400))); match.setPlayerList(List.of(player));
    }

    @Test void firstPassIncludesAllLinkedMembersThenStopsNonParticipants() {
        match.setPlayerList(List.of());
        when(matches.findFootbarSyncWindow(any(), any())).thenReturn(List.of(match));
        assertEquals(Set.of(1L), planner.plan(now).get(0).teamIds());
        state.setLastSuccessAt(now);
        assertTrue(planner.plan(now).isEmpty());
    }

    @Test void retriesMissingParticipantAndStopsWhenSessionArrives() {
        state.setLastSuccessAt(now.minusSeconds(300));
        when(matches.findFootbarSyncWindow(any(), any())).thenReturn(List.of(match));
        assertEquals(1, planner.plan(now).size());
        when(sessions.existsByFootbarAccount_IdAndMatch_Id(2L, 3L)).thenReturn(true);
        assertTrue(planner.plan(now).isEmpty());
    }

    @Test void queryLimitsRetriesToHourAfterSixtyMinuteMatch() {
        assertTrue(planner.plan(now).isEmpty());
        verify(matches).findFootbarSyncWindow(Date.from(now.minusSeconds(7200)), Date.from(now.minusSeconds(3600)));
    }

    @Test void newConnectionImportsHistoryWithoutAnyRecentMatch() {
        account.setLinkedAt(now);
        var work = planner.plan(now).get(0);
        assertTrue(work.history());
        assertEquals(now, work.connectionAt());
        state.setImportedConnectionAt(now);
        assertTrue(planner.plan(now).isEmpty());
    }

    @Test void disconnectedAccountIsNotRetriedAfterOwnerWasWarned() {
        account.setRefreshToken(null);
        assertEquals(1, planner.plan(now).size());
        state.setReconnectRequired(true);
        state.setLastAttemptAt(now);
        assertTrue(planner.plan(now).isEmpty());
        account.setRefreshToken("new-token"); account.setLinkedAt(now);
        assertTrue(planner.plan(now).get(0).history());
    }

    @Test void officialFixtureUsesConfirmedAttendanceBeforeLocalMatchExists() {
        TeamEntity footballTeam = new TeamEntity(); footballTeam.setId(10L); team.setTeam(footballTeam);
        FootballMatchEntity fixture = new FootballMatchEntity(); fixture.setId(12L);
        fixture.setDate(match.getDate()); fixture.setHomeTeam(footballTeam); fixture.setAwayTeam(footballTeam);
        when(fixtures.findFootbarSyncWindow(any(), any())).thenReturn(List.of(fixture));
        state.setLastSuccessAt(now.minusSeconds(300));
        MatchParticipationEntity response = new MatchParticipationEntity(); response.setStatus(MatchParticipationStatus.ATTENDING);
        when(participation.findByFootballMatchIdAndAppTeamIdAndPlayerId(12L, 1L, 7L)).thenReturn(Optional.of(response));
        assertEquals(1, planner.plan(now).size());
        when(sessions.existsForTimeWindow(eq(2L), any(), any())).thenReturn(true);
        assertTrue(planner.plan(now).isEmpty());
    }
}
