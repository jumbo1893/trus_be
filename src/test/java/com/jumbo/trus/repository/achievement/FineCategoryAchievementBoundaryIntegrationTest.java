package com.jumbo.trus.repository.achievement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class FineCategoryAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @ParameterizedTest
    @CsvSource({
        "MISSED_PENALTY,OVERKICK,OWN_GOAL,true",
        "FORGOTTEN_THINGS,INCOMPLETE_EQUIPMENT,OWN_GOAL,true",
        "LATE_BEFORE_START,YELLOW_CARD,MISSED_PENALTY,true",
        "NO_SHOW,RED_CARD,MISSED_PENALTY,true",
        "LATE_AFTER_START,NO_SHOW,RED_CARD,false",
        "YELLOW_CARD,RED_CARD,OVERKICK,false",
        "LATE_BEFORE_START,LATE_AFTER_START,LATE_AFTER_TEN_MINUTES,false",
        "OVERKICK,OWN_GOAL,WEDDING,false"
    })
    void badDayCountsSevenDistinctCategoriesNotFineRows(String first, String second, String third, boolean expected) {
        fine(player, match, first, 1); fine(player, match, second, 1); fine(player, match, third, 1);
        em.flush();
        assertThat(repository.findMatchWherePlayerReceivedAtLeastXFines(player.getId(), match.getId()) != null).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0,false", "1,true", "2,true"})
    void badDayIgnoresZeroQuantitiesAndOtherPlayers(int count, boolean expected) {
        fine(player, match, "OVERKICK", 5); fine(player, match, "OWN_GOAL", 5);
        fine(player, match, "MISSED_PENALTY", count); fine(teammate, match, "YELLOW_CARD", 1);
        em.flush();
        assertThat(repository.findMatchWherePlayerReceivedAtLeastXFines(player.getId(), match.getId()) != null).isEqualTo(expected);
    }
}
