package com.jumbo.trus.service.notification.push.job;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.repository.notification.push.SentPushNotificationRepository;
import com.jumbo.trus.service.achievement.calendar.CzechCelebrationCalendar;
import com.jumbo.trus.service.notification.push.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CelebrationPushJob {
    private static final ZoneId ZONE = ZoneId.of("Europe/Prague");
    static final String TITLE = "Všechno nejlepší!";
    static final String BIRTHDAY = "Všechno nejlepší k narozeninám. Hodně štěstí, zdraví a hlavně Trusu.";
    static final String NAMEDAY = "Všechno nejlepší k svátku. Hodně štěstí, zdraví a hlavně Trusu.";
    private final DeviceTokenRepository tokens;
    private final SentPushNotificationRepository sent;
    private final CzechCelebrationCalendar calendar;
    private final PushService push;
    private Clock clock = Clock.system(ZONE);

    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Prague")
    @Transactional
    public void sendDailyGreetings() {
        if (!sent.tryGreetingJobLock()) return;
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        Date from = Date.from(today.atStartOfDay(ZONE).toInstant());
        Date until = Date.from(today.plusDays(1).atStartOfDay(ZONE).toInstant());
        Set<String> seenTokens = new HashSet<>();
        Set<String> seenDevices = new HashSet<>();
        for (DeviceToken token : tokens.findDistinctByStatusOrderByModificationTimeDesc("ACTIVE")) {
            UserEntity user = token.getUser();
            if (user == null || token.getToken() == null || token.getToken().isBlank()) continue;
            if (!seenTokens.add(token.getToken())) continue;
            String deviceId = token.getClientDeviceId();
            if (deviceId != null && deviceId.isBlank()) deviceId = null;
            if (deviceId != null && !seenDevices.add(user.getId() + ":" + deviceId)) continue;
            for (String body : greetings(user, today)) {
                try {
                    if (sent.wasGreetingSent(user.getId(), token.getToken(), deviceId, TITLE, body, from, until)) continue;
                    push.sendPush(token, TITLE, body, NotificationType.GLOBAL,
                            Map.of("screenId", "home-screen", "type", NotificationType.GLOBAL.name()));
                } catch (Exception e) {
                    log.warn("Greeting push failed for userId={}, deviceTokenId={}", user.getId(), token.getId(), e);
                }
            }
        }
    }

    List<String> greetings(UserEntity user, LocalDate today) {
        boolean birthday = false, nameday = false;
        for (var role : user.getTeamRoles()) {
            PlayerEntity player = role.getPlayer();
            if (player == null || player.isDeleted()) continue;
            if (player.getBirthday() != null) {
                // Date may be a JDBC Date (whose toInstant() is unsupported).
                LocalDate birth = Instant.ofEpochMilli(player.getBirthday().getTime()).atZone(ZONE).toLocalDate();
                MonthDay day = MonthDay.from(birth);
                if (!day.equals(MonthDay.of(1, 1)) && !birth.isAfter(today)
                        && day.equals(MonthDay.from(today))) birthday = true;
            }
            String name = player.getFootballPlayer() == null ? null : player.getFootballPlayer().getName();
            if (calendar.nameDay(today, name).isPresent()) nameday = true;
        }
        List<String> bodies = new ArrayList<>();
        if (birthday) bodies.add(BIRTHDAY);
        if (nameday) bodies.add(NAMEDAY);
        return bodies;
    }
}
