package com.jumbo.trus.service.notification.push.maker;

import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.achievement.AchievementProgressMessage;
import com.jumbo.trus.service.notification.push.PushService;
import com.jumbo.trus.service.transaction.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementProgressNotificationMaker {

    private static final int MAX_PUSH_BODY_LENGTH = 900;

    private final PushService pushService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AfterCommitExecutor afterCommitExecutor;

    public void sendNotifications(List<AchievementProgressMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<AchievementProgressMessage> messagesToSend = messages.stream()
                .filter(Objects::nonNull)
                .filter(message -> message.playerId() != null)
                .filter(message -> message.achievementName() != null)
                .filter(message -> message.missingText() != null)
                .toList();
        if (messagesToSend.isEmpty()) {
            return;
        }

        afterCommitExecutor.execute(
                "achievement-progress-pushes count=" + messagesToSend.size(),
                () -> sendNow(messagesToSend)
        );
    }

    private void sendNow(List<AchievementProgressMessage> messages) {
        Map<Long, List<AchievementProgressMessage>> messagesByPlayer = messages.stream()
                .sorted(Comparator.comparing(AchievementProgressMessage::achievementName))
                .collect(Collectors.groupingBy(
                        AchievementProgressMessage::playerId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        messagesByPlayer.forEach((playerId, playerMessages) -> {
            List<DeviceToken> tokens = distinctActiveTokens(
                    deviceTokenRepository.findDeviceTokensByPlayerId(playerId, "ACTIVE")
            );
            if (tokens.isEmpty()) {
                return;
            }

            String title = buildTitle(playerMessages.size());
            String body = buildBody(playerMessages);
            Map<String, String> data = new HashMap<>();
            data.put("screenId", "view-player-screen");
            data.put("notificationType", NotificationType.ACHIEVEMENT_PROGRESS.name());
            data.put("navigateText", "Chci se podívat");
            data.put("playerId", playerId.toString());

            for (DeviceToken token : tokens) {
                sendPushSafe(token, title, body, data);
            }
        });
    }

    private String buildBody(List<AchievementProgressMessage> messages) {
        String body;
        if (messages.size() == 1) {
            AchievementProgressMessage message = messages.get(0);
            body = "K achievementu „" + message.achievementName() + "“ ti "
                    + message.missingText() + ".";
        } else {
            body = messages.stream()
                    .map(message -> "• " + message.achievementName() + ": "
                            + capitalize(message.missingText()) + ".")
                    .collect(Collectors.joining("\n"));
        }

        if (body.length() <= MAX_PUSH_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_PUSH_BODY_LENGTH - 3) + "...";
    }

    private String buildTitle(int count) {
        if (count == 1) {
            return "Achievement máš na dosah!";
        }
        if (count >= 2 && count <= 4) {
            return "Máš na dosah " + count + " achievementy!";
        }
        return "Máš na dosah " + count + " achievementů!";
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void sendPushSafe(
            DeviceToken token,
            String title,
            String body,
            Map<String, String> data
    ) {
        try {
            pushService.sendPush(
                    token,
                    title,
                    body,
                    NotificationType.ACHIEVEMENT_PROGRESS,
                    data
            );
        } catch (Exception e) {
            log.error(
                    "Achievement progress push failed. deviceTokenId={}",
                    token.getId(),
                    e
            );
        }
    }

    private List<DeviceToken> distinctActiveTokens(List<DeviceToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        Map<String, DeviceToken> tokensByValue = new LinkedHashMap<>();
        for (DeviceToken token : tokens) {
            if (token == null || token.getToken() == null || token.getToken().isBlank()) {
                continue;
            }
            tokensByValue.putIfAbsent(token.getToken(), token);
        }
        return new ArrayList<>(tokensByValue.values());
    }
}
