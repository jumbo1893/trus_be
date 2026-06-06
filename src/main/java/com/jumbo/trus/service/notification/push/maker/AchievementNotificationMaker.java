package com.jumbo.trus.service.notification.push.maker;

import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementNotificationMaker {

    private static final int MAX_PUSH_BODY_LENGTH = 900;

    private final PushService pushService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AfterCommitExecutor afterCommitExecutor;
    private final AchievementDetailService achievementDetailService;

    public void sendAchievementNotify(PlayerAchievementDTO playerAchievement, AppTeamEntity appTeam) {
        sendAchievementNotify(List.of(playerAchievement), appTeam);
    }

    public void sendAchievementNotify(List<PlayerAchievementDTO> playerAchievements, AppTeamEntity appTeam) {
        List<PlayerAchievementDTO> achievementsToSend = playerAchievements == null
                ? List.of()
                : playerAchievements.stream()
                .filter(Objects::nonNull)
                .filter(playerAchievement -> playerAchievement.getPlayer() != null)
                .filter(playerAchievement -> playerAchievement.getAchievement() != null)
                .filter(playerAchievement -> playerAchievement.getAchievement().getName() != null)
                .toList();

        if (achievementsToSend.isEmpty()) {
            return;
        }

        Long appTeamId = appTeam == null ? null : appTeam.getId();
        String description = "achievement-push-batch appTeamId=" + appTeamId
                + ", achievements=" + achievementsToSend.size()
                + ", players=" + countDistinctPlayers(achievementsToSend);

        afterCommitExecutor.execute(description, () -> {
            if (appTeam == null) {
                log.warn("Achievement push batch skipped because appTeam is null. achievements={}", achievementsToSend.size());
                return;
            }
            sendAchievementNotifyNow(achievementsToSend, appTeam);
        });
    }

    private void sendAchievementNotifyNow(List<PlayerAchievementDTO> playerAchievements, AppTeamEntity appTeam) {
        Map<Long, List<PlayerAchievementDTO>> achievementsByPlayer = groupAchievementsByPlayer(playerAchievements);
        if (achievementsByPlayer.isEmpty()) {
            return;
        }

        List<DeviceToken> appTeamDeviceTokens = distinctActiveTokens(
                deviceTokenRepository.findDeviceTokensByAppTeamIdAndStatus(appTeam.getId(), "ACTIVE")
        );

        sendPersonalAchievementPushes(achievementsByPlayer, appTeam);
        sendAppTeamAchievementSummaryPush(appTeamDeviceTokens, achievementsByPlayer);
    }

    private void sendPersonalAchievementPushes(Map<Long, List<PlayerAchievementDTO>> achievementsByPlayer, AppTeamEntity appTeam) {
        for (Map.Entry<Long, List<PlayerAchievementDTO>> entry : achievementsByPlayer.entrySet()) {
            Long playerId = entry.getKey();
            List<PlayerAchievementDTO> playerAchievements = entry.getValue();
            List<DeviceToken> playerDeviceTokens = distinctActiveTokens(
                    deviceTokenRepository.findDeviceTokensByPlayerId(playerId, "ACTIVE")
            );

            if (playerDeviceTokens.isEmpty()) {
                continue;
            }

            String title = playerAchievements.size() == 1
                    ? "Vysloužil sis nový achievement!"
                    : "Vysloužil sis " + playerAchievements.size() + " nové achievementy!";
            String body = buildPlayerAchievementBody(playerAchievements, appTeam);
            Map<String, String> data = getStringStringMap(playerId, NotificationType.PLAYER_ACHIEVEMENT);

            for (DeviceToken deviceToken : playerDeviceTokens) {
                sendPushSafe(deviceToken, title, body, NotificationType.PLAYER_ACHIEVEMENT, data);
            }
        }
    }

    private void sendAppTeamAchievementSummaryPush(
            List<DeviceToken> appTeamDeviceTokens,
            Map<Long, List<PlayerAchievementDTO>> achievementsByPlayer
    ) {
        if (appTeamDeviceTokens.isEmpty()) {
            return;
        }

        Set<Long> achievedPlayerIds = achievementsByPlayer.keySet();
        List<DeviceToken> recipientTokens = appTeamDeviceTokens.stream()
                .filter(token -> !isTokenLinkedToAnyPlayer(token, achievedPlayerIds))
                .toList();

        if (recipientTokens.isEmpty()) {
            return;
        }

        int achievementsCount = achievementsByPlayer.values().stream().mapToInt(List::size).sum();
        String title = achievementsCount == 1
                ? "Nový achievement v týmu!"
                : "Nové achievementy v týmu!";
        String body = buildAppTeamAchievementBody(achievementsByPlayer);
        Long firstPlayerId = achievementsByPlayer.keySet().stream().findFirst().orElse(null);
        Map<String, String> data = getStringStringMap(firstPlayerId, NotificationType.APP_TEAM_ACHIEVEMENT);
        data.put("playersCount", String.valueOf(achievementsByPlayer.size()));
        data.put("achievementsCount", String.valueOf(achievementsCount));

        for (DeviceToken deviceToken : recipientTokens) {
            sendPushSafe(deviceToken, title, body, NotificationType.APP_TEAM_ACHIEVEMENT, data);
        }
    }

    private void sendPushSafe(
            DeviceToken deviceToken,
            String title,
            String body,
            NotificationType notificationType,
            Map<String, String> data
    ) {
        try {
            log.debug("Sending achievement push. deviceTokenId={}, type={}", deviceToken.getId(), notificationType);
            pushService.sendPush(deviceToken, title, body, notificationType, data);
        } catch (Exception e) {
            log.error("Achievement push failed. deviceTokenId={}, type={}", deviceToken.getId(), notificationType, e);
        }
    }

    private Map<Long, List<PlayerAchievementDTO>> groupAchievementsByPlayer(List<PlayerAchievementDTO> playerAchievements) {
        return playerAchievements.stream()
                .sorted(Comparator
                        .comparing((PlayerAchievementDTO playerAchievement) -> nullSafeLower(playerAchievement.getPlayer().getName()))
                        .thenComparing(playerAchievement -> nullSafeLower(playerAchievement.getAchievement().getName())))
                .collect(Collectors.groupingBy(
                        playerAchievement -> playerAchievement.getPlayer().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private String nullSafeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private List<DeviceToken> distinctActiveTokens(List<DeviceToken> deviceTokens) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return List.of();
        }

        Map<String, DeviceToken> tokensByValue = new LinkedHashMap<>();
        for (DeviceToken deviceToken : deviceTokens) {
            if (deviceToken == null || deviceToken.getToken() == null || deviceToken.getToken().isBlank()) {
                continue;
            }

            DeviceToken previous = tokensByValue.putIfAbsent(deviceToken.getToken(), deviceToken);
            if (previous != null && !Objects.equals(previous.getId(), deviceToken.getId())) {
                log.warn("Duplicate active device token skipped. keptDeviceTokenId={}, skippedDeviceTokenId={}",
                        previous.getId(), deviceToken.getId());
            }
        }
        return new ArrayList<>(tokensByValue.values());
    }

    private boolean isTokenLinkedToAnyPlayer(DeviceToken deviceToken, Set<Long> playerIds) {
        UserEntity user = deviceToken.getUser();
        if (user == null || user.getTeamRoles() == null || playerIds == null || playerIds.isEmpty()) {
            return false;
        }

        return user.getTeamRoles().stream()
                .map(UserTeamRole::getPlayer)
                .filter(Objects::nonNull)
                .map(player -> player.getId())
                .anyMatch(playerIds::contains);
    }

    private String buildPlayerAchievementBody(List<PlayerAchievementDTO> playerAchievements, AppTeamEntity appTeam) {
        if (playerAchievements.size() == 1) {
            PlayerAchievementDTO playerAchievement = playerAchievements.get(0);
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
        else {
            String body = playerAchievements.stream()
                    .map(playerAchievement -> "• " + playerAchievement.getAchievement().getName())
                    .collect(Collectors.joining("\n"));
            return truncateBody(body);
        }
    }

    private String buildAppTeamAchievementBody(Map<Long, List<PlayerAchievementDTO>> achievementsByPlayer) {
        String body = achievementsByPlayer.values().stream()
                .map(this::buildAppTeamAchievementLine)
                .collect(Collectors.joining("\n"));
        return truncateBody(body);
    }

    private String buildAppTeamAchievementLine(List<PlayerAchievementDTO> playerAchievements) {
        PlayerAchievementDTO firstAchievement = playerAchievements.get(0);
        String playerName = firstAchievement.getPlayer().getName();
        String achievements = playerAchievements.stream()
                .map(playerAchievement -> playerAchievement.getAchievement().getName())
                .collect(Collectors.joining(", "));
        return playerName + ": " + achievements;
    }

    private String truncateBody(String body) {
        if (body == null || body.length() <= MAX_PUSH_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_PUSH_BODY_LENGTH - 3) + "...";
    }

    private static int countDistinctPlayers(List<PlayerAchievementDTO> playerAchievements) {
        return (int) playerAchievements.stream()
                .map(playerAchievement -> playerAchievement.getPlayer().getId())
                .distinct()
                .count();
    }

    private static @NotNull Map<String, String> getStringStringMap(Long playerId, NotificationType type) {
        Map<String, String> data = new HashMap<>();
        data.put("screenId", "view-player-screen");
        data.put("notificationType", type.name());
        data.put("navigateText", "Chci se podívat");
        if (playerId != null) {
            data.put("playerId", playerId.toString());
        }
        return data;
    }

    private String getFanOrPlayerPercentageText(PlayerAchievementDTO playerAchievement) {
        if (playerAchievement.getAchievement().isOnlyForPlayers()) {
            return "% hráčů";
        } else return "% hráčů a fanoušků";
    }
}
