package com.jumbo.trus.service.websocket;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketSender {

    private final PlayerStatsFacade playerStatsFacade;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendPlayerStatsUpdate(Long playerId, AppTeamEntity appTeam) {
        messagingTemplate.convertAndSend("/topic/player/stats" + playerId, playerStatsFacade.setupPlayerStats(playerId, appTeam, true));
    }
}
