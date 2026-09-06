package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.*;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.achievement.AchievementCalculationScope;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import com.jumbo.trus.service.membership.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AchievementAwardAuditCalculatorTest {
    AchievementCalculator calculator;
    PlayerAchievementRepository repo;
    SeasonService seasons;
    AppTeamEntity team = new AppTeamEntity();
    PlayerAchievementDTO award;

    @BeforeEach void setup() throws Exception {
        var constructor = AchievementCalculator.class.getDeclaredConstructors()[0];
        calculator = (AchievementCalculator) constructor.newInstance(Arrays.stream(constructor.getParameterTypes())
                .map(type -> mock(type)).toArray());
        repo = (PlayerAchievementRepository) ReflectionTestUtils.getField(calculator, "playerAchievementRepository");
        seasons = (SeasonService) ReflectionTestUtils.getField(calculator, "seasonService");
        var achievement = new AchievementDTO(); achievement.setCode("AUTICKO");
        achievement.setCalculationScope(AchievementCalculationScope.MATCH);
        var player = new PlayerDTO(); player.setId(7L);
        award = new PlayerAchievementDTO(achievement, player, true);
        var original = new MatchDTO(); original.setId(10L); award.setMatch(original);
        team.setId(1L);
    }

    @Test void checksAnotherMatchBeforeRevokingAndNeverSendsPushOrCredits() {
        var projection = mock(IMatchIdNumberOneNumberTwo.class);
        when(repo.findAutickoInMatch(7L, 20L)).thenReturn(projection);
        var matchService = (MatchService) ReflectionTestUtils.getField(calculator, "matchService");
        var alternative = new MatchDTO(); alternative.setId(20L);
        when(matchService.getMatch(20L)).thenReturn(alternative);
        var result = calculator.auditAward(award, team, List.of(20L, 10L));
        assertThat(result.getAccomplished()).isTrue();
        assertThat(result.getMatch().getId()).isEqualTo(20L);
        var order = inOrder(repo); order.verify(repo).findAutickoInMatch(7L, 10L);
        order.verify(repo).findAutickoInMatch(7L, 20L);
        verifyNoInteractions((AchievementNotificationMaker) ReflectionTestUtils.getField(calculator, "achievementNotificationMaker"),
                (MembershipService) ReflectionTestUtils.getField(calculator, "membershipService"));
    }

    @Test void invalidAutickoFails() {
        assertThat(calculator.auditAward(award, team, List.of(10L, 20L)).getAccomplished()).isFalse();
    }

    @Test void mecenAsInUnfinishedSeasonFailsEvenWithoutStoredSeasonId() {
        award.getAchievement().setCode("MECENAS");
        award.getAchievement().setCalculationScope(AchievementCalculationScope.SEASON);
        ReflectionTestUtils.setField(calculator, "seasonClock", Clock.fixed(Instant.parse("2026-09-06T10:00:00Z"), ZoneOffset.UTC));
        var season = new SeasonDTO(30L, "Podzim 2026", Date.from(Instant.parse("2026-09-01T00:00:00Z")),
                Date.from(Instant.parse("2026-12-31T22:59:59Z")));
        when(seasons.getAll(any())).thenReturn(List.of(season));
        when(seasons.getSeason(30L)).thenReturn(season);
        assertThat(calculator.auditAward(award, team, List.of()).getAccomplished()).isFalse();
    }

    @Test void manualAndOtherAchievementsAreSkipped() {
        award.getAchievement().setManually(true);
        assertThat(calculator.auditAward(award, team, List.of())).isNull();
        award.getAchievement().setManually(false);
        award.getAchievement().setCalculationScope(AchievementCalculationScope.OTHER);
        assertThat(calculator.auditAward(award, team, List.of())).isNull();
        verifyNoInteractions(repo);
    }
}
