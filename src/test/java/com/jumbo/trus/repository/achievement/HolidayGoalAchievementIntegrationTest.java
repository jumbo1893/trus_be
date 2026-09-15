package com.jumbo.trus.repository.achievement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.service.achievement.calendar.CzechCelebrationCalendar;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;

class HolidayGoalAchievementIntegrationTest extends AchievementDatabaseFixture {
    @Test
    void queryIsTeamPlayerAndMatchScopedAndPreservesGoals() throws Exception {
        match.setDate(Date.from(Instant.parse("2026-04-03T12:00:00Z")));
        goal(player, match, 2, 1);
        goal(teammate, match, 5, 0);
        var otherMatch = match(); goal(player, otherMatch, 0, 2);
        em.flush();
        var facts = repository.findHolidayGoalCandidates(player.getId(), team.getId(), null);
        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).getMatchId()).isEqualTo(match.getId());
        assertThat(facts.get(0).getGoals()).isEqualTo(2);
        assertThat(repository.findHolidayGoalCandidates(player.getId(), -1L, null)).isEmpty();
        assertThat(repository.findHolidayGoalCandidates(player.getId(), team.getId(), otherMatch.getId())).isEmpty();
        ReflectionTestUtils.setField(calculator, "celebrationCalendar", new CzechCelebrationCalendar(new ObjectMapper()));
        var result = calculate("SVATECNI_STRELEC", player, match);
        assertThat(result.getAccomplished()).isTrue();
        assertThat(result.getDetail()).contains("Velký pátek", "Počet gólů: 2");
        assertThat(result.getMatch().getId()).isEqualTo(match.getId());
        player.setFan(true); em.flush();
        assertThat(repository.findHolidayGoalCandidates(player.getId(), team.getId(), null)).isEmpty();
    }
}
