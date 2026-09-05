package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.StepUpdateRepository;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import com.jumbo.trus.service.achievement.StepAchievementCalculator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class StepAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @Autowired StepUpdateRepository steps;
    @Autowired FootbarSessionRepository sessions;
    @Autowired MatchRepository matches;
    private StepAchievementCalculator stepCalculator() { return new StepAchievementCalculator(steps, sessions, matches); }

    @org.junit.jupiter.api.Test
    void strengthSavingUsesMaximumDistanceInsteadOfAddingDuplicateMeasurements() {
        UserEntity user = walkingUser(player);
        LocalDate date = match.getDate().toInstant().atZone(ZoneId.of("Europe/Prague")).toLocalDate();
        step(user, date.minusDays(1), 5000);
        step(user, date.minusDays(2), 0);
        footbar(player, match).setDistance(2000.0);
        footbar(player, match).setDistance(3000.0);
        em.flush();
        assertThat(stepCalculator().calculateStrengthSaving(player.getId(), team.getId(), match.getId())).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"OKOLO_HRADCE,65000", "PRAZAK,160000", "OD_SEVERU_K_JIHU,341000",
        "OD_VYCHODU_NA_ZAPAD,612000", "VSECHNY_CESTY_VEDOU_DO_RIMA,1600000",
        "EVROPSKY_POCHUZKAR,7200000", "CESTA_KOLEM_SVETA,51380000"})
    void everyStepMilestoneTestsBelowExactlyAndAboveWithRealDailyReports(String code, int threshold) {
        UserEntity user = walkingUser(player);
        LocalDate day = LocalDate.of(2024, 1, 1);
        step(user, day, threshold - 1);
        assertThat(stepCalculator().milestoneResult(player.getId(), team.getId(), threshold)).as(code + " below").isEmpty();
        step(user, day.plusDays(1), 1);
        assertThat(stepCalculator().milestoneResult(player.getId(), team.getId(), threshold)).as(code + " exact")
                .hasValueSatisfying(result -> assertThat(result.stepCount()).isEqualTo(threshold));
        step(user, day.plusDays(2), 1);
        assertThat(stepCalculator().milestoneResult(player.getId(), team.getId(), threshold)).as(code + " above")
                .hasValueSatisfying(result -> assertThat(result.stepCount()).isEqualTo(threshold));
    }

    @ParameterizedTest
    @CsvSource({"1999,1000,2,false", "2000,1000,2,true", "3900,5000,2,false", "3901,5000,2,true",
        "4000,5000,1,false", "4000,5000,0,false"})
    void strengthSavingNeedsTwoReportedDaysAndStrictlyMoreDistance(double meters, int count, int days, boolean expected) {
        UserEntity user = walkingUser(player);
        LocalDate date = match.getDate().toInstant().atZone(ZoneId.of("Europe/Prague")).toLocalDate();
        if (days > 0) step(user, date.minusDays(1), count);
        if (days > 1) step(user, date.minusDays(2), 0);
        // Match day and earlier history must not influence the two-day comparison.
        step(user, date, 90000); step(user, date.minusDays(3), 90000);
        footbar(player, match).setDistance(meters); em.flush();
        assertThat(stepCalculator().calculateStrengthSaving(player.getId(), team.getId(), match.getId()).isPresent()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"2,100,99,false", "3,100,99,true", "3,100,100,true", "3,99,100,false", "3,0,100,false"})
    void walkerNeedsThreePositiveWalkersAndSharedMaximum(int walkers, int count, int others, boolean expected) {
        UserEntity user = walkingUser(player);
        var nextMatch = match(player);
        LocalDate date = match.getDate().toInstant().atZone(ZoneId.of("Europe/Prague")).toLocalDate();
        step(user, date, count);
        for (int i = 1; i < walkers; i++) step(walkingUser(player(false)), date, others);
        assertThat(stepCalculator().calculateWalker(player.getId(), team.getId(), nextMatch.getId()).isPresent()).isEqualTo(expected);
    }

    private UserEntity walkingUser(PlayerEntity who) {
        UserEntity user = new UserEntity(); user.setMail(UUID.randomUUID() + "@test.invalid");
        user.setPassword("test-only"); user.setName("Test walker"); save(user);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team); role.setPlayer(who); role.setRole("READER"); save(role);
        StepConsentEntity consent = new StepConsentEntity(); consent.setUser(user); consent.setAppTeam(team);
        consent.setEnabled(true); consent.setUpdatedAt(Instant.now()); save(consent);
        return user;
    }
    private void step(UserEntity user, LocalDate date, int count) {
        StepUpdateEntity step = new StepUpdateEntity(); step.setUser(user); step.setDate(date); step.setStepNumber(count);
        step.setSource(StepSource.HEALTH_CONNECT); step.setTimezone("Europe/Prague");
        step.setMeasuredUntil(date.atStartOfDay().atOffset(ZoneOffset.UTC)); step.setUpdateTime(Instant.now()); save(step);
    }
}
