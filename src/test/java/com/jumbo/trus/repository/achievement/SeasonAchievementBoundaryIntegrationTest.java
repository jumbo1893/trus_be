package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;

class SeasonAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @ParameterizedTest
    @CsvSource({"1,2,false", "2,2,true", "3,2,true", "0,0,false"})
    void scorerComparesOnlyGoalsWithinTheSelectedSeason(int goals, int otherGoals, boolean expected) {
        var season = season(); match.setSeason(season);
        goal(player, match, goals, 0); goal(teammate, match, otherGoals, 30);
        var outside = match(player, teammate);
        outside.setSeason(season()); goal(teammate, outside, 100, 0);
        assertThat(repository.findStrelecInSeason(player.getId(), season.getId(), team.getId()) != null).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"2,1,4,false", "2,2,4,true", "2,3,4,true", "0,0,0,false"})
    void drinkingAverageUsesBeersPlusShotsAndCountsSoberAttendance(int beers, int shots, int otherDrinks, boolean expected) {
        var season = season(); match.setSeason(season);
        beer(player, match, beers, shots); beer(teammate, match, otherDrinks, 0);
        // Both players have two attendances, but only one beer record each.
        var sober = match(player, teammate); sober.setSeason(season); em.flush();
        assertThat(repository.findKdyzLejuTakPoradneInSeason(player.getId(), season.getId(), team.getId()) != null).isEqualTo(expected);
        var outside = match(player); outside.setSeason(season()); beer(player, outside, 100, 0);
        assertThat(repository.findKdyzLejuTakPoradneInSeason(player.getId(), season.getId(), team.getId()) != null).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"3,2,false", "4,2,true", "5,2,true", "0,1,false", "5,0,false"})
    void beersPerGoalRequiresBothPositiveCountsAndUsesRatio(int beers, int goals, boolean expected) {
        var season = season(); match.setSeason(season);
        beer(player, match, beers, 100); goal(player, match, goals, 100);
        beer(teammate, match, 2, 0); goal(teammate, match, 1, 0);
        assertThat(repository.findGolyNeRadejiPivoInSeason(player.getId(), season.getId(), team.getId()) != null).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"2,false", "3,true", "4,true"})
    void alarmClockRequiresThreeLateArrivalsInOneSeason(int count, boolean expected) {
        var season = season(); match.setSeason(season);
        fine(player, match, FineCodes.LATE_AFTER_START, count);
        var outside = match(player); outside.setSeason(season()); fine(player, outside, FineCodes.LATE_AFTER_START, 10);
        assertThat(repository.findFirstMatchInSeasonWithLateArrival(player.getId(), season.getId()) != null).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"1,1,true", "1,0,false", "0,1,false"})
    void yellowIsGoodMayBeFulfilledInDifferentMatchesButNotDifferentSeasons(int bathroom, int yellow, boolean expected) {
        var season = season(); match.setSeason(season);
        fine(player, match, FineCodes.BATHROOM_DURING_MATCH, bathroom);
        var second = match(player); second.setSeason(season); fine(player, second, FineCodes.YELLOW_CARD, yellow);
        var outside = match(player); outside.setSeason(season()); fine(player, outside, FineCodes.YELLOW_CARD, 10);
        assertThat(calculateSeason("ZLUTA_JE_DOBRA", season).getAccomplished()).isEqualTo(expected);
    }
    @ParameterizedTest
    @CsvSource({"0,4,false", "0,5,true", "1,5,true", "2,5,false", "1,6,true"})
    void teamPlayerNeedsFiveAssistsAndAtMostOneMissedMatch(int missed, int assists, boolean expected) {
        var season = season(); match.setSeason(season);
        goal(player, match, 0, assists);
        for (int i = 0; i < missed; i++) { var absent = match(teammate); absent.setSeason(season); }
        var outside = match(teammate); outside.setSeason(season()); em.flush();
        assertThat(repository.findTeamPlayerInSeason(player.getId(), season.getId(), team.getId()) != null).isEqualTo(expected);
    }
    private SeasonEntity season() {
        SeasonEntity season = new SeasonEntity(); season.setAppTeam(team); season.setName("Boundary season");
        season.setFromDate(new Date(1600000000000L)); season.setToDate(new Date(1800000000000L));
        return save(season);
    }
}
