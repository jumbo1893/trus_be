package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.service.achievement.AchievementCodes;
import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;

class HistoryAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @Test
    void narodSeIsFirstAttendanceNotBirthdayFineAndIsNotRepeated() {
        assertMatch(AchievementCodes.NAROD_SE, true);
        var later = match(player);
        fine(player, later, FineCodes.CHILD_BORN_BOY, 1);
        assertThat(calculate(AchievementCodes.NAROD_SE, player, later).getAccomplished()).isFalse();
    }
    @ParameterizedTest
    @CsvSource({"0,false", "1,true", "2,true"})
    void debutGoalMustActuallyBeScoredOnTheFirstAttendance(int goals, boolean expected) {
        goal(player, match, goals, 0);
        assertMatch(AchievementCodes.NASTUP_JAKO_HROM, expected);
        var later = match(player); goal(player, later, 1, 0);
        assertThat(calculate(AchievementCodes.NASTUP_JAKO_HROM, player, later).getAccomplished()).isFalse();
    }
    @ParameterizedTest
    @CsvSource({"2,1,false", "3,1,true", "4,1,true", "3,0,false"})
    void consistencyNeedsThreeScoringAppearancesNotAssists(int appearances, int lastGoals, boolean expected) {
        MatchEntity current = match;
        for (int i = 0; i < appearances; i++) {
            if (i > 0) current = match(player);
            goal(player, current, i == appearances - 1 ? lastGoals : 1, 3);
        }
        assertThat(calculate(AchievementCodes.KONZISTENCE, player, current).getAccomplished()).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"2,2,false", "3,2,true", "4,2,true", "3,3,false"})
    void tahounNeedsThreeTopDrinkingResultsAndAcceptsSharedFirst(int appearances, int otherLastDrinks, boolean expected) {
        MatchEntity current = match;
        for (int i = 0; i < appearances; i++) {
            if (i > 0) current = match(player, teammate);
            beer(player, current, 1, 1);
            beer(teammate, current, i == appearances - 1 ? otherLastDrinks : 2, 0);
        }
        assertThat(calculate(AchievementCodes.TAHOUN, player, current).getAccomplished()).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"4,0,0,false", "5,0,0,true", "6,0,0,true", "5,1,0,false", "5,0,1,false"})
    void doPoctuRequiresFivePointlessAppearances(int appearances, int lastGoals, int lastAssists, boolean expected) {
        SeasonEntity season = new SeasonEntity(); season.setName("Season"); season.setAppTeam(team);
        season.setFromDate(new Date(1600000000000L)); season.setToDate(new Date(1800000000000L)); save(season);
        MatchEntity current = match;
        MatchEntity fifth = null;
        for (int i = 0; i < appearances; i++) {
            if (i > 0) current = match(player);
            current.setSeason(season);
            goal(player, current, i == appearances - 1 ? lastGoals : 0, i == appearances - 1 ? lastAssists : 0);
            if (i == 4) fifth = current;
        }
        // This milestone belongs to the fifth game, even after a sixth pointless game.
        assertThat(calculate(AchievementCodes.DO_POCTU, player, fifth == null ? current : fifth).getAccomplished()).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"YELLOW_CARD,-1,false", "YELLOW_CARD,0,false", "YELLOW_CARD,1,true", "RED_CARD,1,true"})
    void fotrJeLotrNeedsCardStrictlyAfterFirstChildbirth(String cardCode, int dayOffset, boolean expected) {
        fine(player, match, FineCodes.CHILD_BORN_GIRL, 1);
        MatchEntity card = dayOffset == 0 ? match : match(player);
        card.setDate(new Date(match.getDate().getTime() + dayOffset * 86400000L));
        fine(player, card, cardCode, 1);
        assertThat(repository.findFotrJeLotr(player.getId(), team.getId()) != null).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"RABONA_GOAL,1,true", "RABONA_GOAL,0,false", "RABONA,1,false"})
    void machyrekRequiresActualRabonaGoalFineNotJustRabona(String fineCode, int count, boolean expected) {
        fine(player, match, fineCode, count);
        assertMatch(AchievementCodes.MACHYREK, expected);
    }
}
