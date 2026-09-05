package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.service.achievement.AchievementCodes;
import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class TeamAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @ParameterizedTest
    @CsvSource({"true,true", "false,false"})
    void klubSracuIncludesFansAmongEverybodyWhoMustSkip(boolean fanSkipped, boolean expected) {
        PlayerEntity fan = player(true); match.getPlayerList().add(fan); em.flush();
        fine(player, match, FineCodes.THIRD_HALF, 1); fine(teammate, match, FineCodes.THIRD_HALF, 1);
        if (fanSkipped) fine(fan, match, FineCodes.THIRD_HALF, 1);
        assertMatch(AchievementCodes.KLUB_SRACU, expected);
    }
    @ParameterizedTest
    @CsvSource({"true,false,true", "true,true,true", "false,true,false", "false,false,false"})
    void lonelyStayerIncludesFansAndNeedsNoDrinkRecord(boolean fanSkipped, boolean drinkRecorded, boolean expected) {
        PlayerEntity fan = player(true); match.getPlayerList().add(fan); em.flush();
        fine(teammate, match, FineCodes.THIRD_HALF, 1);
        if (fanSkipped) fine(fan, match, FineCodes.THIRD_HALF, 1);
        if (drinkRecorded) beer(player, match, 1, 0);
        assertMatch(AchievementCodes.OSAMELY_DRZAK, expected);
    }
    @ParameterizedTest
    @CsvSource({"true,1,true", "false,1,false", "true,0,false"})
    void twoStayersMustBeExactlyTwoIncludingFansAndBothHaveBeer(boolean fanSkipped, int otherBeers, boolean expected) {
        PlayerEntity fan = player(true); match.getPlayerList().add(fan); em.flush();
        if (fanSkipped) fine(fan, match, FineCodes.THIRD_HALF, 1);
        beer(player, match, 1, 0); beer(teammate, match, otherBeers, 1);
        assertMatch(AchievementCodes.VE_DVOU_SE_TO_LEPE_TAHNE, expected);
    }
    @ParameterizedTest
    @CsvSource({"true,true", "false,false"})
    void theSecondStayerMayBeAFan(boolean fanHasBeer, boolean expected) {
        PlayerEntity fan = player(true); match.getPlayerList().add(fan); em.flush();
        fine(teammate, match, FineCodes.THIRD_HALF, 1);
        beer(player, match, 1, 0); beer(fan, match, fanHasBeer ? 1 : 0, 0);
        assertMatch(AchievementCodes.VE_DVOU_SE_TO_LEPE_TAHNE, expected);
    }
    @ParameterizedTest
    @CsvSource({"true,false,false", "false,false,true", "false,true,false"})
    void moralSupportMeansLocalAttendanceWithoutFootballRosterEntry(boolean onRoster, boolean fan, boolean expected) {
        var entry = performance(player, match, 0, false, false);
        if (!onRoster) em.remove(entry);
        player.setFan(fan); em.flush();
        assertMatch(AchievementCodes.MORALNI_PODPORA, expected);
    }
    @ParameterizedTest
    @CsvSource({"2,false", "3,true", "4,true"})
    void lazarNeedsThreeUnlistedAttendancesWithinSelectedSeason(int count, boolean expected) {
        SeasonEntity season = new SeasonEntity(); season.setName("Lazar season"); season.setAppTeam(team);
        season.setFromDate(new Date(1600000000000L)); season.setToDate(new Date(1800000000000L)); save(season);
        var current = match;
        for (int i = 0; i < count; i++) {
            if (i > 0) current = match(player);
            current.setSeason(season);
            var entry = performance(player, current, 0, false, false); em.remove(entry); em.flush();
        }
        assertThat(calculateSeason(AchievementCodes.LAZAR_NA_TRIBUNACH, season).getAccomplished()).isEqualTo(expected);
    }
}
