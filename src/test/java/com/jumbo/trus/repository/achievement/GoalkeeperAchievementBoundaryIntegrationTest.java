package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.service.achievement.AchievementCodes;
import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GoalkeeperAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @ParameterizedTest
    @CsvSource({"0,1,false,false", "1,0,false,false", "1,1,false,true", "1,1,true,true", "90,2,false,true"})
    void modernGoalkeeperNeedsAnyMinutesAndAnAssistNotCleanSheet(int minutes, int assists, boolean clean, boolean expected) {
        performance(player, match, minutes, false, clean);
        goal(player, match, 0, assists);
        assertMatch(AchievementCodes.MODERNI_GOLMANSKA_SKOLA, expected);
    }

    @ParameterizedTest
    @CsvSource({"0,true,1,false", "1,true,1,true", "1,false,1,false", "1,true,0,false", "90,true,1,true"})
    void dopingNeedsHangoverAndPersonalCleanSheetWithSomeGoalkeeping(int minutes, boolean clean, int hangover, boolean expected) {
        performance(player, match, minutes, false, clean);
        fine(player, match, FineCodes.HANGOVER, hangover);
        assertMatch(AchievementCodes.DOPING, expected);
    }

    @ParameterizedTest
    @CsvSource({"2,1,false", "3,1,true", "4,1,true", "3,0,false"})
    void dopingAlsoAcceptsRecordedHattrickWithoutExternalRoster(int goals, int hangover, boolean expected) {
        goal(player, match, goals, 0);
        fine(player, match, FineCodes.HANGOVER, hangover);
        assertMatch(AchievementCodes.DOPING, expected);
    }

    @ParameterizedTest
    @CsvSource({"0,true,0,1,1,1,false", "1,true,0,1,1,1,true", "1,false,0,1,1,1,false",
            "1,true,0,0,1,1,false", "1,true,0,1,0,1,false", "1,true,0,1,1,0,false",
            "0,false,1,1,1,1,true"})
    void successfulDayRequiresEveryPartAndPersonalNotTeamCleanSheet(int minutes, boolean clean, int goals,
            int beers, int shots, int yellow, boolean expected) {
        performance(player, match, minutes, false, clean);
        goal(player, match, goals, 0); beer(player, match, beers, shots);
        fine(player, match, FineCodes.YELLOW_CARD, yellow);
        assertMatch(AchievementCodes.USPESNY_DEN, expected);
    }
}
