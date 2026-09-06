package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.OutboxEventRepository;
import com.jumbo.trus.entity.outbox.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class AchievementDataReadinessIntegrationTest extends AchievementDatabaseFixture {
    @Autowired MatchRepository matches;
    @Autowired OutboxEventRepository events;

    @Test void emptyRosterDoesNotProveAbsenceAndOneOfficialPlayerIsEnough() {
        var entry = performance(player, match, 0, false, false); em.remove(entry); em.flush();
        assertThat(repository.findMoralniPodporaInMatch(player.getId(), match.getId())).isNull();
        assertThat(repository.findMoralniPodpora(player.getId(), team.getId())).isNull();
        performance(teammate, match, 0, false, false);
        assertThat(repository.findMoralniPodporaInMatch(player.getId(), match.getId())).isNotNull();
        assertThat(repository.findMoralniPodpora(player.getId(), team.getId())).isNotNull();
    }

    @Test void lazarCountsOnlyMatchesWithKnownRoster() {
        var season = new SeasonEntity(); season.setAppTeam(team); season.setName("Roster readiness");
        season.setFromDate(new Date(0)); season.setToDate(new Date()); save(season);
        for (int i = 0; i < 3; i++) {
            var m = i == 2 ? match : match(player); m.setSeason(season);
            var entry = performance(player, m, 0, false, false); em.remove(entry); em.flush();
            if (i < 2) performance(teammate, m, 0, false, false);
        }
        assertThat(repository.findLazarNaTribune(player.getId(), team.getId(), season.getId())).isNull();
        performance(teammate, match, 0, false, false);
        assertThat(repository.findLazarNaTribune(player.getId(), team.getId(), season.getId())).isNotNull();
    }

    @Test void waitsForOneHourOrBothScoresIncludingZeroZero() {
        Instant now = Instant.parse("2026-09-06T10:00:00Z");
        match.setDate(Date.from(now.minusSeconds(3599))); em.flush();
        assertThat(matches.findAwaitingFinalStatistics(team.getId(), match.getId(), null, Date.from(now.minusSeconds(3600)), Date.from(now))).hasSize(1);
        match.setDate(Date.from(now.minusSeconds(3600))); em.flush();
        assertThat(matches.findAwaitingFinalStatistics(team.getId(), match.getId(), null, Date.from(now.minusSeconds(3600)), Date.from(now))).isEmpty();
        match.setDate(Date.from(now.minusSeconds(60))); match.setHomeGoalNumber(0); em.flush();
        assertThat(matches.findAwaitingFinalStatistics(team.getId(), match.getId(), null, Date.from(now.minusSeconds(3600)), Date.from(now))).hasSize(1);
        match.setAwayGoalNumber(0); em.flush();
        assertThat(matches.findAwaitingFinalStatistics(team.getId(), match.getId(), null, Date.from(now.minusSeconds(3600)), Date.from(now))).isEmpty();
        match.setHomeGoalNumber(null); match.setAwayGoalNumber(null);
        performance(player, match, 0, false, false);
        match.getFootballMatch().setHomeGoalNumber(0); match.getFootballMatch().setAwayGoalNumber(0); em.flush();
        assertThat(matches.findAwaitingFinalStatistics(team.getId(), match.getId(), null, Date.from(now.minusSeconds(3600)), Date.from(now))).isEmpty();
    }

    @Test void newDeferredEventBecomesReadyAtItsScheduledTime() {
        Instant due = Instant.parse("1900-01-01T00:00:00Z");
        var event = new OutboxEventEntity(); event.setAppTeamId(team.getId());
        event.setAggregateId(match.getId()); event.setAggregateType(OutboxAggregateType.ALL);
        event.setEventType(OutboxEventType.MATCH_ACHIEVEMENTS_READY);
        event.setCreatedAt(due.minusSeconds(3600)); event.setNextAttemptAt(due);
        event.setPayload(new OutboxEventPayload(match.getId(), null, null, null, null, null)); save(event);
        assertThat(events.findReadyForProcessing(OutboxEventStatus.NEW, OutboxEventStatus.RETRY, due.minusSeconds(1), PageRequest.of(0, 100)))
                .extracting(OutboxEventEntity::getId).doesNotContain(event.getId());
        assertThat(events.findReadyForProcessing(OutboxEventStatus.NEW, OutboxEventStatus.RETRY, due, PageRequest.of(0, 100)))
                .extracting(OutboxEventEntity::getId).contains(event.getId());
    }
}
