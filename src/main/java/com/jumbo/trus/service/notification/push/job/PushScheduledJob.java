package com.jumbo.trus.service.notification.push.job;

import com.jumbo.trus.entity.notification.push.NotificationPair;
import com.jumbo.trus.repository.notification.push.PushNotificationRepository;
import com.jumbo.trus.service.notification.push.MatchNotificationMaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PushScheduledJob {

    private final MatchNotificationMaker matchNotificationMaker;

    private final PushNotificationRepository pushNotificationRepository;

    @Scheduled(cron = "0 0 9-23 * * *")
    public void findUpcomingMatches() {
        log.debug("spuštěn findUpcomingMatches job ");
        LocalDateTime now = LocalDateTime.now();
        Date from = Date.from(now.minusDays(7).atZone(ZoneId.systemDefault()).toInstant());
        Date to = Date.from(now.plusDays(3).atZone(ZoneId.systemDefault()).toInstant());
        List<NotificationPair> pairs = pushNotificationRepository.findNotificationPairs(from, to);
        matchNotificationMaker.sendMatchNotify(pairs);
    }

}