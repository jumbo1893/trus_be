package com.jumbo.trus.service.notification.push.maker;

import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.achievement.AchievementDetailService;
import com.jumbo.trus.service.notification.push.PushService;
import com.jumbo.trus.service.transaction.AfterCommitExecutor;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementNotificationMaker {

    private final PushService pushService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AchievementDetailService achievementDetailService;
    private final AfterCommitExecutor afterCommitExecutor;

    public void sendAchievementNotify(PlayerAchievementDTO playerAchievement, AppTeamEntity appTeam) {
        long playerAchievementId = playerAchievement.getId();
        Long playerId = playerAchievement.getPlayer() == null ? null : playerAchievement.getPlayer().getId();
        Long appTeamId = appTeam == null ? null : appTeam.getId();

        afterCommitExecutor.execute(
                "achievement-push playerAchievementId=" + playerAchievementId + ", playerId=" + playerId + ", appTeamId=" + appTeamId,
                () -> {
                    assert appTeam != null;
                    sendAchievementNotifyNow(playerAchievement, appTeam);
                }
        );
    }

    private void sendAchievementNotifyNow(PlayerAchievementDTO playerAchievement, AppTeamEntity appTeam) {
        List<DeviceToken> deviceTokenList = deviceTokenRepository.findDeviceTokensByAppTeamIdAndStatus(appTeam.getId(), "ACTIVE");
        List<DeviceToken> playerDeviceTokenList = deviceTokenRepository.findDeviceTokensByPlayerId(playerAchievement.getPlayer().getId(), "ACTIVE");
        String allTitle = playerAchievement.getPlayer().isFan()
                ? "Fanouškovi " + playerAchievement.getPlayer().getName() + " byl připsán achievement!"
                : "Hráči " + playerAchievement.getPlayer().getName() + " byl připsán achievement!";
        String playerTitle = "Vysloužil sis nový achievement!";
        String body = buildAchievementPushBody(playerAchievement, appTeam);
        /*for (DeviceToken deviceToken : deviceTokenList) {
            log.debug(deviceToken.getToken());
            try {
                Map<String, String> data = getStringStringMap(playerAchievement.getPlayer().getId(), NotificationType.APP_TEAM_ACHIEVEMENT);
                pushService.sendPush(deviceToken, allTitle, body, NotificationType.APP_TEAM_ACHIEVEMENT, data);
            } catch (Exception e) {
                log.error("error:", e);
            }
        }*/
        for (DeviceToken deviceToken : playerDeviceTokenList) {
            log.debug(deviceToken.getToken());
            try {
                Map<String, String> data = getStringStringMap(playerAchievement.getPlayer().getId(), NotificationType.PLAYER_ACHIEVEMENT);
                pushService.sendPush(deviceToken, playerTitle, body, NotificationType.PLAYER_ACHIEVEMENT, data);
            } catch (Exception e) {
                log.error("error:", e);
            }
        }
    }

    private static @NotNull Map<String, String> getStringStringMap(Long playerId, NotificationType type) {
        Map<String, String> data = new java.util.HashMap<>();
        data.put("screenId", "view-player-screen");
        data.put("notificationType", type.name());
        data.put("navigateText", "Chci se podívat");
        data.put("playerId", playerId.toString());
        return data;
    }

    private String buildAchievementPushBody(PlayerAchievementDTO playerAchievement, AppTeamEntity appTeam) {
        AchievementDetail achievementDetail =
                achievementDetailService.getAchievementDetail(playerAchievement, appTeam);
        int successRate = Math.round(achievementDetail.getSuccessRate() * 100);
        String achievementName = playerAchievement.getAchievement().getName();
        String body;
        if (successRate < 10) {
            body = "Velká gratulace! Jedná se o velmi raritní achievement " + achievementName + ", který zatím splnilo pouze " + successRate + getFanOrPlayerPercentageText(playerAchievement);
        } else if (successRate < 30) {
            body = "Dlouhé úsilí se vyplatilo! Jedná se o raritní achievement " + achievementName + ", který zatím splnilo pouze " + successRate + getFanOrPlayerPercentageText(playerAchievement);
        } else if (successRate < 50) {
            body = "Jedná se o velmi zajímavý achievement " + achievementName + ", který zatím splnilo pouze " + successRate + getFanOrPlayerPercentageText(playerAchievement);
        } else if (successRate < 70) {
            body = achievementName + " zařazuje mezi legendy, tedy mezi ostatních " + successRate + getFanOrPlayerPercentageText(playerAchievement);
        } else if (successRate < 90) {
            body = "Dlouho to trvalo, protože " + achievementName + " splnilo již " + successRate + getFanOrPlayerPercentageText(playerAchievement);
        } else {
            body = "Pověstný kůl v plotě již nestojí, protože " + achievementName + " splnilo dalších " + successRate + getFanOrPlayerPercentageText(playerAchievement);
        }
        return body;
    }

    private String getFanOrPlayerPercentageText(PlayerAchievementDTO playerAchievement) {
        if (playerAchievement.getAchievement().isOnlyForPlayers()) {
            return "% hráčů";
        } else return "% hráčů a fanoušků";
    }
}
