package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.NotificationFootballMatch;
import com.jumbo.trus.entity.notification.push.NotificationPair;
import com.jumbo.trus.entity.notification.push.NotificationType;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.notification.push.NotificationFootballMatchRepository;
import com.jumbo.trus.service.football.helper.FootballMatchFormatter;
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
            NotificationType notificationType = findMatchNotificationToSend(match);
            if (notificationType != null) {
                for (DeviceToken token : entry.getValue()) {
                    try {
                        sendUpcomingMatchNotify(token, match, notificationType);
                    } catch (Exception e) {
                        log.error("error sending push to {}", token, e);
                    }
                }
                // pošle se všem uživatelům → až teď uložíme
                createAndSaveNotificationFootballMatch(match, notificationType);
            }
        }
    }

    private NotificationType findMatchNotificationToSend(FootballMatchEntity footballMatchEntity) {
        Date now = new Date();
        Date matchDate = footballMatchEntity.getDate();
        List<NotificationFootballMatch> notificationFootballMatches = notificationFootballMatchRepository.findByFootballMatch(footballMatchEntity);

        long diffMillis = matchDate.getTime() - now.getTime();
        long diffHours = diffMillis / (1000 * 60 * 60); // převod na hodiny
        if (diffHours <= 24 && diffHours >= 0 && isMatchNotNotified(NotificationType.ONE_DAY_BEFORE, notificationFootballMatches)) {
            if (isMatchNotNotified(NotificationType.THREE_DAYS_BEFORE, notificationFootballMatches)) {
                createAndSaveNotificationFootballMatch(footballMatchEntity, NotificationType.THREE_DAYS_BEFORE);
            }
            return NotificationType.ONE_DAY_BEFORE;

        } else if (diffHours > 24 && isMatchNotNotified(NotificationType.THREE_DAYS_BEFORE, notificationFootballMatches)) {
            return NotificationType.THREE_DAYS_BEFORE;
        } else if (footballMatchEntity.getHomeGoalNumber() != null && isMatchNotNotified(NotificationType.AFTER_RESULT, notificationFootballMatches)) {
            return NotificationType.AFTER_RESULT;
        } else if (footballMatchEntity.getRefereeComment() != null && isMatchNotNotified(NotificationType.REFEREE_COMMENT, notificationFootballMatches)) {
            return NotificationType.REFEREE_COMMENT;
        } else {
            return null;
        }
    }

    private boolean isMatchNotNotified(NotificationType notificationType, List<NotificationFootballMatch> notificationFootballMatches) {
        return notificationFootballMatches.stream()
                .noneMatch(n -> n.getType() == notificationType && n.isSent());
    }

    private void sendUpcomingMatchNotify(DeviceToken deviceToken, FootballMatchEntity footballMatch, NotificationType notificationType) throws Exception {
        String title = getPushTitle(notificationType);
        String body = getPushBody(notificationType, footballMatchMapper.toDTO(footballMatch));
        pushService.sendPush(deviceToken, title, body);
    }

    private String getPushTitle(NotificationType notificationType) {
        return switch (notificationType) {
            case THREE_DAYS_BEFORE -> "Blíží se zápas";
            case ONE_DAY_BEFORE -> "Méně 24 hodin do zápasu";
            case AFTER_RESULT -> "Odehraný zápas";
            case REFEREE_COMMENT -> "Byl připsán komentář sudího";
        };
    }

    private String getPushBody(NotificationType notificationType, FootballMatchDTO footballMatch) {
        return switch (notificationType) {
            case ONE_DAY_BEFORE -> FootballMatchFormatter.toStringWithDateAndStadium(footballMatch);
            case THREE_DAYS_BEFORE -> FootballMatchFormatter.toStringWithDateAndStadium(footballMatch) + "\n Nezapomeň potvrdit účast!";
            case AFTER_RESULT -> FootballMatchFormatter.toStringWithResult(footballMatch);
            case REFEREE_COMMENT -> FootballMatchFormatter.toStringWithResultAndRefereeComment(footballMatch);
        };
    }

    private void createAndSaveNotificationFootballMatch(FootballMatchEntity footballMatch, NotificationType notificationType) {
        NotificationFootballMatch notificationFootballMatch = new NotificationFootballMatch();
        notificationFootballMatch.setFootballMatch(footballMatch);
        notificationFootballMatch.setType(notificationType);
        notificationFootballMatch.setSent(true);
        notificationFootballMatchRepository.save(notificationFootballMatch);
    }
}
