package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class LifetimeAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @ParameterizedTest(name = "{0}: threshold={1}, beers={2}")
    @CsvSource({"JEDNOU_SE_ZACIT_MUSI,1,true", "KDYZ_ONO_TO_CHUTNA,50,true", "SOUDEK,100,true",
        "CISTERNA,500,true", "PRITVRDIME,1,false", "RUMOVY_NADENIK,20,false", "ACHIEVEMENT_MILANA_CURDY,50,false"})
    void drinkMilestonesAreNotAwardedBelowThresholdAndRemainAwardedAbove(String code, int threshold, boolean beers) {
        beer(player, match, beers ? threshold - 1 : 2000, beers ? 2000 : threshold - 1);
        beer(teammate, match, 2000, 2000);
        assertThat(repository.findDrinkMilestone(player.getId(), team.getId(), beers ? threshold : 0, beers ? 0 : threshold))
                .as(code + " below threshold; other drink and other player do not count").isNull();
        var crossing = match(player);
        beer(player, crossing, beers ? 1 : 0, beers ? 0 : 1);
        var atThreshold = repository.findDrinkMilestone(player.getId(), team.getId(), beers ? threshold : 0, beers ? 0 : threshold);
        assertThat(atThreshold).as(code + " at threshold").isNotNull();
        assertThat(atThreshold.getMatchId()).isEqualTo(crossing.getId());
        beer(player, match(player), 1, 1);
        assertThat(repository.findDrinkMilestone(player.getId(), team.getId(), beers ? threshold : 0, beers ? 0 : threshold).getMatchId())
                .as("first crossing is retained above threshold").isEqualTo(crossing.getId());
    }
    @ParameterizedTest
    @CsvSource({"PERMICE_NA_TRUS,10", "ULTRUS,30"})
    void fanMilestonesRequireTenOrThirtyDistinctAttendancesAndFanStatus(String code, int threshold) {
        player.setFan(true); em.flush();
        for (int i = 1; i < threshold - 1; i++) match(player);
        assertThat(repository.findFanAttendanceMilestone(player.getId(), team.getId(), threshold)).as(code).isNull();
        var crossing = match(player);
        assertThat(repository.findFanAttendanceMilestone(player.getId(), team.getId(), threshold).getMatchId()).isEqualTo(crossing.getId());
        match(player);
        assertThat(repository.findFanAttendanceMilestone(player.getId(), team.getId(), threshold).getMatchId()).isEqualTo(crossing.getId());
        player.setFan(false); em.flush();
        assertThat(repository.findFanAttendanceMilestone(player.getId(), team.getId(), threshold)).isNull();
    }
    @ParameterizedTest
    @CsvSource({"AMERICKY_FOTBALISTA,OVERKICK,10", "ROSS_GELLER,WEDDING,3"})
    void cumulativeFinesCountQuantitiesNotRowsAndIgnoreOtherFineCodes(String code, String fineCode, int threshold) {
        fine(player, match, fineCode, threshold - 1);
        fine(player, match, FineCodes.NEW_BOOTS, 20);
        fine(teammate, match, fineCode, 20);
        assertThat(repository.findFineMilestone(player.getId(), team.getId(), List.of(fineCode), threshold)).as(code).isNull();
        var crossing = match(player);
        fine(player, crossing, fineCode, 1);
        assertThat(repository.findFineMilestone(player.getId(), team.getId(), List.of(fineCode), threshold).getMatchId()).isEqualTo(crossing.getId());
        fine(player, match(player), fineCode, 1);
        assertThat(repository.findFineMilestone(player.getId(), team.getId(), List.of(fineCode), threshold).getMatchId()).isEqualTo(crossing.getId());
    }
}
