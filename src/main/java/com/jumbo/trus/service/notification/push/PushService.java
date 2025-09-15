package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.SentPushNotification;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.notification.push.SentPushNotificationRepository;
import com.jumbo.trus.service.football.helper.FootballMatchFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PushService {

    private final FcmService fcmService;
    private final DeviceTokenCollector deviceTokenCollector;
    private final FootballMatchRepository footballMatchRepository;
    private final FootballMatchMapper footballMatchMapper;
    private final SentPushNotificationRepository sentPushNotificationRepository;

    public DeviceTokenDTO addNewToken(DeviceTokenDTO deviceTokenDTO) {
        return deviceTokenCollector.addNewToken(deviceTokenDTO);
    }

    public void sendPush(DeviceToken deviceToken, String title, String body) throws Exception {
        if (fcmService.sendPush(deviceToken, title, body)) {
            saveSentPushToRepository(title, body, deviceToken);
        }
    }

    public void sendTestPush() {
        FootballMatchEntity footballMatch = footballMatchRepository.findById(16043L).orElseThrow();
        TeamEntity team = footballMatch.getHomeTeam();
        sendUpcomingMatchNotify(team, footballMatchMapper.toDTO(footballMatch));
    }

    public void sendUpcomingMatchNotify(TeamEntity teamEntity, FootballMatchDTO footballMatch) {
        List<Long> userIds = teamEntity.getAppTeams().stream()
                .flatMap(appTeam -> appTeam.getTeamRoles().stream())
                .map(UserTeamRole::getUser)
                .map(UserEntity::getId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        String title = "Za 24 hodin je zápas";
        String body = FootballMatchFormatter.toStringWithDateAndStadium(footballMatch);
        deviceTokenCollector.getTokensByUserList(userIds).forEach(
                deviceToken -> {
                    try {
                        sendPush(deviceToken, title, body);
                    } catch (Exception ignored) {

                    }
                }
        );
    }

    private void saveSentPushToRepository(String title, String body, DeviceToken deviceToken) {
        SentPushNotification sentPushNotification = new SentPushNotification(
                deviceToken,
                title,
                body,
                new Date(),
                "SENT");
        sentPushNotificationRepository.save(sentPushNotification);
    }
}
