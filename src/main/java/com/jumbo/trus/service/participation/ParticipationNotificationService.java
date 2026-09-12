package com.jumbo.trus.service.participation;

import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import com.jumbo.trus.service.notification.push.PushService;
import com.jumbo.trus.service.transaction.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipationNotificationService {
    private final AfterCommitExecutor afterCommit;
    private final DeviceTokenRepository tokens;
    private final MatchParticipationRepository participations;
    private final PushService push;

    public void notifyChange(Long actorUserId, Long teamId, Long matchId, String action, String comment) {
        // Never announce a rolled-back response. Read recipients and total after commit.
        afterCommit.execute("participation-push match=" + matchId, () -> {
            var attending = participations.findAllByFootballMatchIdAndAppTeamIdOrderByPlayerNameAsc(matchId, teamId)
                    .stream().filter(p -> p.getStatus() == MatchParticipationStatus.ATTENDING
                            && !p.getPlayer().isDeleted()).toList();
            long playing = attending.stream().filter(p -> p.getPlaying() == null
                    ? !p.getPlayer().isFan() : p.getPlaying()).count();
            String body = action + ". Hrajících účastníků: " + playing
                    + ". Celkem se účastní: " + attending.size() + ".";
            if (comment != null && !comment.isBlank()) {
                String text = comment.trim();
                body += "\n" + (text.length() > 600 ? text.substring(0, 600) + "…" : text);
            }
            Map<String, String> data = Map.of("screenId", "match-participation-screen",
                    "footballMatchId", matchId.toString(), "appTeamId", teamId.toString(),
                    "notificationType", "MATCH_PARTICIPATION", "navigateText", "Zobrazit účast");
            var sent = new HashSet<String>();
            for (var token : tokens.findDeviceTokensByAppTeamIdAndStatus(teamId, "ACTIVE")) {
                if (token.getUser() == null || Objects.equals(token.getUser().getId(), actorUserId)
                        || token.getToken() == null || token.getToken().isBlank() || !sent.add(token.getToken())) continue;
                try {
                    push.sendPush(token, "Účast na zápase", body, NotificationType.MATCH_PARTICIPATION, data);
                } catch (Exception e) {
                    log.warn("Participation push failed for device {}", token.getId(), e);
                }
            }
        });
    }
}
