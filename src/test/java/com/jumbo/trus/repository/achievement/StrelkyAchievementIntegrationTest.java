package com.jumbo.trus.repository.achievement;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StrelkyAchievementIntegrationTest extends AchievementDatabaseFixture {
    @Test
    void sameMatchAwardsWithExactGoalAndAssistCountsAndHistoricalLookup() {
        goal(player, match, 2, 3);
        fine(player, match, "NEW_BOOTS", 1);
        fine(player, match, "NEW_BOOTS", 1);
        var award = calculate("STRELKY", player, match);
        assertThat(award.getAccomplished()).isTrue();
        assertThat(award.getMatch().getId()).isEqualTo(match.getId());
        assertThat(award.getDetail()).isEqualTo("Počet gólů: 2, počet asistencí: 3.");
        assertThat(repository.findStrelky(player.getId(), team.getId(), null).getMatchId()).isEqualTo(match.getId());
        assertThat(repository.findStrelky(player.getId(), -1L, null)).isNull();
    }

    @Test
    void fineInAnotherMatchOrForAnotherPlayerDoesNotCount() {
        goal(player, match, 1, 0);
        fine(player, match(), "NEW_BOOTS", 1);
        fine(teammate, match, "NEW_BOOTS", 1);
        assertMatch("STRELKY", false);
        assertThat(repository.findStrelky(player.getId(), team.getId(), null)).isNull();
    }

    @Test
    void requiresPositiveFineAndGoalButNoAssist() {
        goal(player, match, 0, 2);
        fine(player, match, "NEW_BOOTS", 1);
        assertMatch("STRELKY", false);
        var second = match();
        goal(player, second, 1, 0);
        fine(player, second, "NEW_BOOTS", 0);
        assertThat(calculate("STRELKY", player, second).getAccomplished()).isFalse();
        fine(player, second, "NEW_BOOTS", 1);
        assertThat(calculate("STRELKY", player, second).getAccomplished()).isTrue();
    }

    @Test
    void fanCannotReceiveAward() {
        player.setFan(true);
        goal(player, match, 1, 0);
        fine(player, match, "NEW_BOOTS", 1);
        assertMatch("STRELKY", false);
        assertThat(repository.findStrelky(player.getId(), team.getId(), null)).isNull();
    }
}
