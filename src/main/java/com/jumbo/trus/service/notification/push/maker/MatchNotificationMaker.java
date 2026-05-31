package com.jumbo.trus.service.notification.push.maker;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.NotificationPair;
import com.jumbo.trus.entity.notification.push.match.NotificationFootballMatch;
import com.jumbo.trus.entity.notification.push.match.NotificationMatchType;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.notification.push.NotificationFootballMatchRepository;
import com.jumbo.trus.service.football.helper.FootballMatchFormatter;
import com.jumbo.trus.service.notification.push.PushService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchNotificationMaker {

    private final NotificationFootballMatchRepository notificationFootballMatchRepository;
    private final PushService pushService;
    private final FootballMatchMapper footballMatchMapper;

    public void sendMatchNotify(List<NotificationPair> pairs) {
        // seskupit podle zápasu
        Map<FootballMatchEntity, List<DeviceToken>> grouped = pairs.stream()
                .collect(Collectors.groupingBy(NotificationPair::getFootballMatch,
                        Collectors.mapping(NotificationPair::getDeviceToken, Collectors.toList())));

        for (Map.Entry<FootballMatchEntity, List<DeviceToken>> entry : grouped.entrySet()) {
            FootballMatchEntity match = entry.getKey();
            NotificationMatchType notificationMatchType = findMatchNotificationToSend(match);
            if (notificationMatchType != null) {
                for (DeviceToken token : entry.getValue()) {
                    try {
                        Map<String, String> data = getStringStringMap(match.getId());
                        sendUpcomingMatchNotify(token, match, notificationMatchType, data);
                    } catch (Exception e) {
                        log.error("error sending push to tokenId {}", token.getId(), e);
                    }
                }
                // pošle se všem uživatelům → až teď uložíme
                createAndSaveNotificationFootballMatch(match, notificationMatchType);
            }
        }
    }

    private static @NotNull Map<String, String> getStringStringMap(Long footballMatchId) {
        Map<String, String> data = new java.util.HashMap<>();
        data.put("screenId", "match-detail-screen");
        data.put("navigateText", "Zobraz detail");
        data.put("footballMatchId", footballMatchId.toString());
        return data;
    }

    private NotificationMatchType findMatchNotificationToSend(FootballMatchEntity footballMatchEntity) {
        Date now = new Date();
        Date matchDate = footballMatchEntity.getDate();
        List<NotificationFootballMatch> notificationFootballMatches = notificationFootballMatchRepository.findByFootballMatch(footballMatchEntity);

        long diffMillis = matchDate.getTime() - now.getTime();
        long diffHours = diffMillis / (1000 * 60 * 60); // převod na hodiny
        if (diffHours <= 24 && diffHours >= 0 && isMatchNotNotified(NotificationMatchType.ONE_DAY_BEFORE, notificationFootballMatches)) {
            if (isMatchNotNotified(NotificationMatchType.THREE_DAYS_BEFORE, notificationFootballMatches)) {
                createAndSaveNotificationFootballMatch(footballMatchEntity, NotificationMatchType.THREE_DAYS_BEFORE);
            }
            return NotificationMatchType.ONE_DAY_BEFORE;

        } else if (diffHours > 24 && isMatchNotNotified(NotificationMatchType.THREE_DAYS_BEFORE, notificationFootballMatches)) {
            return NotificationMatchType.THREE_DAYS_BEFORE;
        } else if (footballMatchEntity.getHomeGoalNumber() != null && isMatchNotNotified(NotificationMatchType.AFTER_RESULT, notificationFootballMatches)) {
            return NotificationMatchType.AFTER_RESULT;
        } else if (footballMatchEntity.getRefereeComment() != null && isMatchNotNotified(NotificationMatchType.REFEREE_COMMENT, notificationFootballMatches)) {
            return NotificationMatchType.REFEREE_COMMENT;
        } else {
            return null;
        }
    }

    private boolean isMatchNotNotified(NotificationMatchType notificationMatchType, List<NotificationFootballMatch> notificationFootballMatches) {
        return notificationFootballMatches.stream()
                .noneMatch(n -> n.getType() == notificationMatchType && n.isSent());
    }

    private void sendUpcomingMatchNotify(DeviceToken deviceToken, FootballMatchEntity footballMatch, NotificationMatchType notificationMatchType,  Map<String, String> data) throws Exception {
        String title = getPushTitle(notificationMatchType);
        String body = getPushBody(notificationMatchType, footballMatchMapper.toDTO(footballMatch));
        pushService.sendPush(deviceToken, title, body, convertMatchTypeToNotificationType(notificationMatchType), data);
    }

    private String getPushTitle(NotificationMatchType notificationMatchType) {
        return switch (notificationMatchType) {
            case THREE_DAYS_BEFORE -> "Blíží se zápas";
            case ONE_DAY_BEFORE -> "Méně 24 hodin do zápasu";
            case AFTER_RESULT -> "Odehraný zápas";
            case REFEREE_COMMENT -> "Byl připsán komentář sudího";
        };
    }

    private String getPushBody(NotificationMatchType notificationMatchType, FootballMatchDTO footballMatch) {
        return switch (notificationMatchType) {
            case ONE_DAY_BEFORE -> FootballMatchFormatter.toStringWithDateAndStadium(footballMatch);
            case THREE_DAYS_BEFORE -> FootballMatchFormatter.toStringWithDateAndStadium(footballMatch) + "\n Nezapomeň potvrdit účast!";
            case AFTER_RESULT -> FootballMatchFormatter.toStringWithResult(footballMatch);
            case REFEREE_COMMENT -> FootballMatchFormatter.toStringWithResultAndRefereeComment(footballMatch);
        };
    }

    private void createAndSaveNotificationFootballMatch(FootballMatchEntity footballMatch, NotificationMatchType notificationMatchType) {
        NotificationFootballMatch notificationFootballMatch = new NotificationFootballMatch();
        notificationFootballMatch.setFootballMatch(footballMatch);
        notificationFootballMatch.setType(notificationMatchType);
        notificationFootballMatch.setSent(true);
        notificationFootballMatchRepository.save(notificationFootballMatch);
    }

    private NotificationType convertMatchTypeToNotificationType(NotificationMatchType notificationMatchType) {
        return switch (notificationMatchType) {
            case ONE_DAY_BEFORE -> NotificationType.ONE_DAY_BEFORE;
            case THREE_DAYS_BEFORE -> NotificationType.THREE_DAYS_BEFORE;
            case AFTER_RESULT -> NotificationType.AFTER_RESULT;
            case REFEREE_COMMENT -> NotificationType.REFEREE_COMMENT;
        };
    }
}
