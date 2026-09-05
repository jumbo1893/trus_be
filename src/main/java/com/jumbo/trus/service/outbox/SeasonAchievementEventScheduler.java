package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.service.achievement.SeasonAchievementTiming;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class SeasonAchievementEventScheduler {
    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;
    private final OutboxEventService outboxEventService;
    private Clock clock = Clock.system(SeasonAchievementTiming.ZONE);

    @Transactional
    public void enqueueDueSeasons() {
        Date tomorrow = Date.from(LocalDate.now(clock.withZone(SeasonAchievementTiming.ZONE))
                .plusDays(1).atStartOfDay(SeasonAchievementTiming.ZONE).toInstant());
        for (var season : seasonRepository.findDueForAchievements(tomorrow)) {
            var matches = matchRepository.findMatchIdsBySeason(season.getId());
            if (!matches.isEmpty()) {
                outboxEventService.createEventForTeam(OutboxEventType.SEASON_ACHIEVEMENTS_DUE,
                        OutboxAggregateType.SEASON, season.getId(),
                        OutboxEventPayloadFactory.seasonUpdated(matches), season.getAppTeam().getId(), null);
            }
            season.setAchievementEventForEnd(season.getToDate());
        }
    }
}
