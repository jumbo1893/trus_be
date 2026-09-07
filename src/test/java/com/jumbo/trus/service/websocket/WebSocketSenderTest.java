package com.jumbo.trus.service.websocket;

import com.jumbo.trus.dto.player.stats.PlayerStats;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketSenderTest {

    @Test
    void sendsTeamScopedUpdateAndKeepsLegacyTopicForOlderApps() {
        PlayerStatsFacade facade = mock(PlayerStatsFacade.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        AppTeamRepository appTeams = mock(AppTeamRepository.class);
        WebSocketSender sender = new WebSocketSender(facade, messaging, appTeams);
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(12L);
        PlayerStats stats = new PlayerStats();
        when(facade.setupPlayerStats(34L, appTeam, true)).thenReturn(stats);

        sender.sendPlayerStatsUpdate(34L, appTeam);

        verify(messaging).convertAndSend("/topic/team/12/player/stats/34", stats);
        verify(messaging).convertAndSend("/topic/player/stats34", stats);
    }
}
