package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.*;
import com.jumbo.trus.entity.footbar.*;
import com.jumbo.trus.repository.footbar.*;
import com.jumbo.trus.service.activity.footbar.connect.FootbarConnect;
import com.jumbo.trus.service.activity.footbar.session.FootbarSessionProcessor;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FootbarSessionPairingTest {
    FootbarAccountRepository accounts = mock(FootbarAccountRepository.class);
    FootbarSessionRepository sessions = mock(FootbarSessionRepository.class);
    FootbarConnect connect = mock(FootbarConnect.class);
    MatchService matches = mock(MatchService.class);
    OutboxEventService events = mock(OutboxEventService.class);
    FootbarSessionProcessor processor;
    FootbarAccountEntity account;
    AppTeamEntity team;
    UserTeamRole role;
    MatchEntity match;
    FootbarSessionDTO dto;

    @BeforeEach void setup() {
        processor = spy(new FootbarSessionProcessor(accounts, sessions, null, connect,
                null, null, null, matches, events));
        team = new AppTeamEntity();
        team.setId(1L);
        account = new FootbarAccountEntity();
        account.setId(1L);
        UserEntity user = new UserEntity();
        role = new UserTeamRole();
        role.setAppTeam(team);
        PlayerEntity player = new PlayerEntity();
        player.setId(7L);
        role.setPlayer(player);
        user.setTeamRoles(List.of(role));
        account.setUser(user);
        match = new MatchEntity();
        match.setId(10L);
        SeasonEntity season = new SeasonEntity();
        season.setId(3L);
        match.setSeason(season);
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        when(connect.getValidAccessToken(account)).thenReturn("token");
        dto = new FootbarSessionDTO();
        dto.setFootbarSessionId(42L);
        doReturn(List.of(dto)).when(processor).fetchSessions("token");
        when(sessions.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"match", "player", "season"})
    void incompletePairingIsSavedAndCanBePairedOnNextSync(String missing) {
        FootbarSessionEntity stored = new FootbarSessionEntity();
        stored.setId(99L);
        when(sessions.findByfootbarSessionIdAndFootbarAccount(42L, account)).thenReturn(Optional.of(stored));
        PlayerEntity player = role.getPlayer();
        SeasonEntity season = match.getSeason();
        if (missing.equals("player")) role.setPlayer(null);
        if (missing.equals("season")) match.setSeason(null);
        when(matches.findMatchByAroundTime(eq(team), any(), any()))
                .thenReturn(missing.equals("match") ? null : match);
        assertDoesNotThrow(() -> processor.saveSessions(account, team));
        verify(sessions).save(stored);
        verifyNoInteractions(events);

        role.setPlayer(player);
        match.setSeason(season);
        when(matches.findMatchByAroundTime(eq(team), any(), any())).thenReturn(match);
        processor.saveSessions(account, team);
        verify(events).createEventForTeam(any(), any(), isNull(), any(), eq(team.getId()), isNull());
        assertSame(player, stored.getPlayer());
        assertSame(match, stored.getMatch());
        processor.saveSessions(account, team);
        verify(events, times(1)).createEventForTeam(any(), any(), isNull(), any(), eq(team.getId()), isNull());
    }

    @Test void missingDetailProducesDescriptiveErrorInsteadOfNullPointer() {
        when(sessions.findByfootbarSessionIdAndFootbarAccount(42L, account)).thenReturn(Optional.empty());
        doReturn(null).when(processor).fetchFootbarSessionDetail(42L, "token");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.saveSessions(account, team));
        assertTrue(error.getMessage().contains("42"));
        verify(sessions, never()).save(any());
        verifyNoInteractions(events);
    }
}
