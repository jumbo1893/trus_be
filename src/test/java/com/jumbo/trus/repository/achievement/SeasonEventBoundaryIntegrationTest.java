package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.service.outbox.SeasonAchievementEventScheduler;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SeasonEventBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @Autowired SeasonRepository seasons;
    @Autowired MatchRepository matches;

    @Test
    void enqueuesOnFinalDayOnceAndAgainAfterEndDateChanges() {
        var season = new SeasonEntity(); season.setName("Final day"); season.setAppTeam(team);
        season.setFromDate(new Date(0)); season.setToDate(Date.from(Instant.parse("2026-06-30T21:59:59Z")));
        save(season); match.setSeason(season); em.flush();
        var events = mock(OutboxEventService.class);
        var scheduler = new SeasonAchievementEventScheduler(seasons, matches, events);
        setTime(scheduler, "2026-06-29T21:59:59Z"); scheduler.enqueueDueSeasons();
        verifyNoInteractions(events);
        setTime(scheduler, "2026-06-29T22:00:00Z"); scheduler.enqueueDueSeasons(); em.flush();
        scheduler.enqueueDueSeasons();
        verify(events, times(1)).createEventForTeam(eq(OutboxEventType.SEASON_ACHIEVEMENTS_DUE), any(),
                eq(season.getId()), argThat(p -> p.relatedEntityIds().values().stream().anyMatch(ids -> ids.contains(match.getId()))),
                eq(team.getId()), isNull());
        assertThat(season.getAchievementEventForEnd()).isEqualTo(season.getToDate());
        season.setToDate(Date.from(Instant.parse("2026-07-31T21:59:59Z"))); em.flush();
        scheduler.enqueueDueSeasons(); verifyNoMoreInteractions(events);
        setTime(scheduler, "2026-07-30T22:00:00Z"); scheduler.enqueueDueSeasons();
        verify(events, times(2)).createEventForTeam(any(), any(), any(), any(), any(), isNull());
    }

    private void setTime(SeasonAchievementEventScheduler scheduler, String now) {
        ReflectionTestUtils.setField(scheduler, "clock", Clock.fixed(Instant.parse(now), ZoneOffset.UTC));
    }
}
