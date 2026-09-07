package com.jumbo.trus.service.websocket;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.service.outbox.AchievementEventBatch;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSender {

    private final PlayerStatsFacade playerStatsFacade;
    private final SimpMessagingTemplate messagingTemplate;
    private final AppTeamRepository appTeamRepository;

    public void sendPlayerStatsUpdate(Long playerId, AppTeamEntity appTeam) {
        var stats = playerStatsFacade.setupPlayerStats(playerId, appTeam, true);
        messagingTemplate.convertAndSend(teamDestination(appTeam.getId(), playerId), stats);

        // Starší verze aplikace poslouchají původní topic bez týmu.
        messagingTemplate.convertAndSend(legacyDestination(playerId), stats);
    }

    public void sendPlayerStatsUpdates(AchievementEventBatch batch) {
        batch.workByTeamAndPlayer().forEach((appTeamId, workByPlayer) ->
                appTeamRepository.findById(appTeamId).ifPresentOrElse(
                        appTeam -> workByPlayer.keySet().forEach(
                                playerId -> sendPlayerStatsUpdate(playerId, appTeam)
                        ),
                        () -> log.warn("Skipping player stats update for missing appTeamId={}", appTeamId)
                )
        );
    }

    static String teamDestination(Long appTeamId, Long playerId) {
        return "/topic/team/" + appTeamId + "/player/stats/" + playerId;
    }

    static String legacyDestination(Long playerId) {
        return "/topic/player/stats" + playerId;
    }
}
