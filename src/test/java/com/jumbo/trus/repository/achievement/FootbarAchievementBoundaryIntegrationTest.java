package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.service.achievement.AchievementCodes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FootbarAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @Test
    void sprintCountUsesMaximumFromOneMeasurementRatherThanSum() {
        var first = footbar(player, match); first.setSprintSpeed(25.0 / 3.6); first.setSprintCount(2);
        var second = footbar(player, match); second.setSprintSpeed(26.0 / 3.6); second.setSprintCount(3);
        em.flush();
        var result = repository.findCerneGenyInMatch(player.getId(), match.getId());
        assertThat(result).isNotNull();
        assertThat(result.getSecondNumber()).isEqualTo(3);
    }

    @Test
    void legacyFullRecalculationQueriesAlsoReturnAttributeMaximums() {
        var first = footbar(player, match); first.setShotSpeed(81.0 / 3.6); first.setPassCount(40);
        var second = footbar(player, match); second.setShotSpeed(90.0 / 3.6); second.setPassCount(55);
        goal(player, match, 1, 0); em.flush();
        assertThat(repository.findRobertoCarlos(player.getId(), team.getId()).getFirstNumber()).isEqualTo(90);
        assertThat(repository.findSpilmachr(player.getId(), team.getId()).getFirstNumber()).isEqualTo(55);
    }
    @ParameterizedTest
    @CsvSource({"3,false", "5,true"})
    void replenishmentUsesHighestDistanceEvenWhenLowerMeasurementWouldQualify(int beers, boolean expected) {
        footbar(player, match).setDistance(3000.0);
        footbar(player, match).setDistance(5000.0);
        beer(player, match, beers, 0);
        assertMatch(AchievementCodes.DOPLNENI_TEKUTIN, expected);
    }
    @Test
    void duplicateMeasurementsUseMaximumNotSumWhenRankingDistance() {
        footbar(player, match).setDistance(2000.0);
        footbar(player, match).setDistance(2000.0);
        footbar(teammate, match).setDistance(3000.0);
        assertMatch(AchievementCodes.JA_TO_ZA_VAS_OBEHAL, false);
    }
    @Test
    void marathonUsesMaximumMeasurementPerMatchNotDuplicateSum() {
        footbar(player, match).setDistance(21000.0);
        footbar(player, match).setDistance(21100.0);
        em.flush();
        assertThat(repository.findMaratonec(player.getId(), team.getId())).isNull();
        footbar(player, match(player)).setDistance(21000.0); em.flush();
        assertThat(repository.findMaratonec(player.getId(), team.getId())).isNotNull();
    }
    @Test
    void passCountsFromSeparateMeasurementsAreNotAdded() {
        footbar(player, match).setPassCount(20);
        footbar(player, match).setPassCount(20);
        assertMatch(AchievementCodes.SPILMACHR, false);
        footbar(player, match).setPassCount(40);
        assertMatch(AchievementCodes.SPILMACHR, true);
    }
    @ParameterizedTest
    @CsvSource({"79.999,1,false", "80,1,false", "80.001,1,true", "81,0,false"})
    void robertoCarlosRequiresStrictlyOverEightyAndGoal(double kmh, int goals, boolean expected) {
        var session = footbar(player, match); session.setShotSpeed(kmh / 3.6);
        goal(player, match, goals, 0);
        assertMatch(AchievementCodes.ROBERTO_CARLOS, expected);
    }
    @ParameterizedTest
    @CsvSource({"39,false", "40,true", "41,true"})
    void spilmachrRequiresAtLeastFortyPasses(int passes, boolean expected) {
        footbar(player, match).setPassCount(passes);
        assertMatch(AchievementCodes.SPILMACHR, expected);
    }
    @ParameterizedTest
    @CsvSource({"24.999,false", "25,true", "25.001,true"})
    void cerneGenyIncludesExactlyTwentyFiveKmh(double kmh, boolean expected) {
        footbar(player, match).setSprintSpeed(kmh / 3.6);
        assertMatch(AchievementCodes.CERNE_GENY, expected);
    }
    @ParameterizedTest
    @CsvSource({"2999,3,0,false", "3000,3,0,true", "3001,3,0,false", "3001,4,0,true", "3000,2,1,false"})
    void replenishmentRequiresThreeKmAndEnoughBeersNotShots(double meters, int beers, int shots, boolean expected) {
        footbar(player, match).setDistance(meters); beer(player, match, beers, shots);
        assertMatch(AchievementCodes.DOPLNENI_TEKUTIN, expected);
    }
    @ParameterizedTest
    @CsvSource({"3000,2999,true,true", "3000,3000,true,true", "3000,3001,true,false", "3000,0,false,false"})
    void mostDistanceNeedsTwoDistinctPlayersAndAcceptsTies(double meters, double otherMeters, boolean other, boolean expected) {
        footbar(player, match).setDistance(meters);
        if (other) footbar(teammate, match).setDistance(otherMeters);
        assertMatch(AchievementCodes.JA_TO_ZA_VAS_OBEHAL, expected);
    }
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void marathonCrosses42100MetersCumulatively(int delta) {
        footbar(player, match).setDistance(21000.0);
        var second = match(player);
        footbar(player, second).setDistance(21100.0 + delta);
        footbar(teammate, match).setDistance(50000.0);
        em.flush();
        var result = repository.findMaratonec(player.getId(), team.getId());
        assertThat(result != null).isEqualTo(delta >= 0);
        if (result != null) assertThat(result.getMatchId()).isEqualTo(second.getId());
    }
}
