package com.jumbo.trus.service.achievement;

import com.jumbo.trus.service.fine.FineCodes;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.achievement.helper.IMatchIdDecimalAndNumber;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.membership.MembershipService;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementRuleBoundaryTest {

    @Mock private BeerService beerService;
    @Mock private AchievementMapper achievementMapper;
    @Mock private AchievementRepository achievementRepository;
    @Mock private PlayerAchievementRepository playerAchievementRepository;
    @Mock private PlayerAchievementMapper playerAchievementMapper;
    @Mock private FootballMatchService footballMatchService;
    @Mock private MatchService matchService;
    @Mock private SeasonService seasonService;
    @Mock private FootballPlayerStatsService footballPlayerStatsService;
    @Mock private ReceivedFineService receivedFineService;
    @Mock private FineService fineService;
    @Mock private GoalService goalService;
    @Mock private AchievementNotificationMaker achievementNotificationMaker;
    @Mock private StepAchievementCalculator stepAchievementCalculator;
    @Mock private MembershipService membershipService;

    @InjectMocks private AchievementCalculator calculator;

    private final PlayerDTO player = player();
    private final AppTeamEntity appTeam = appTeam();

    @BeforeEach
    void setUp() {
        lenient().when(matchService.getMatch(anyLong())).thenAnswer(invocation -> {
            MatchDTO match = new MatchDTO();
            match.setId(invocation.getArgument(0));
            return match;
        });
    }

    static Stream<Arguments> drinkMilestones() {
        return Stream.of(
                Arguments.of(AchievementCodes.JEDNOU_SE_ZACIT_MUSI, 1, 0),
                Arguments.of(AchievementCodes.KDYZ_ONO_TO_CHUTNA, 50, 0),
                Arguments.of(AchievementCodes.SOUDEK, 100, 0),
                Arguments.of(AchievementCodes.CISTERNA, 500, 0),
                Arguments.of(AchievementCodes.PRITVRDIME, 0, 1),
                Arguments.of(AchievementCodes.RUMOVY_NADENIK, 0, 20),
                Arguments.of(AchievementCodes.ACHIEVEMENT_MILANA_CURDY, 0, 50)
        );
    }

    @ParameterizedTest(name = "{0}: piva={1}, panáky={2}")
    @MethodSource("drinkMilestones")
    void cumulativeDrinkMilestonesUseTheThresholdFromTheirDescription(
            String code,
            int beerThreshold,
            int liquorThreshold
    ) {
        IMatchIdNumberOneNumberTwo result = numbers(101L, beerThreshold, liquorThreshold);
        when(playerAchievementRepository.findDrinkMilestone(
                player.getId(), appTeam.getId(), beerThreshold, liquorThreshold))
                .thenReturn(result);

        PlayerAchievementDTO calculated = ReflectionTestUtils.invokeMethod(
                calculator,
                "calculateDrinkMilestoneAchievement",
                player,
                achievement(code),
                appTeam,
                AchievementType.ALL,
                beerThreshold,
                liquorThreshold
        );

        assertAccomplished(calculated, 101L);
        verify(playerAchievementRepository).findDrinkMilestone(
                player.getId(), appTeam.getId(), beerThreshold, liquorThreshold);
    }

    static Stream<Arguments> fineMilestones() {
        return Stream.of(
                Arguments.of(AchievementCodes.AMERICKY_FOTBALISTA, List.of(FineCodes.OVERKICK), 10),
                Arguments.of(AchievementCodes.ALZHEIMER, List.of(FineCodes.FORGOTTEN_THINGS, FineCodes.INCOMPLETE_EQUIPMENT), 1),
                Arguments.of(AchievementCodes.LEO_BERANEK, List.of("Nový kopačky"), 1)
        );
    }

    @ParameterizedTest(name = "{0}: {1} x {2}")
    @MethodSource("fineMilestones")
    void cumulativeFineMilestonesUseTheFineAndCountFromTheirDescription(
            String code,
            List<String> fineNames,
            int threshold
    ) {
        when(playerAchievementRepository.findFineMilestone(
                player.getId(), appTeam.getId(), fineNames, threshold))
                .thenReturn(numbers(102L, threshold, threshold));

        PlayerAchievementDTO calculated = ReflectionTestUtils.invokeMethod(
                calculator,
                "calculateFineMilestoneAchievement",
                player,
                achievement(code),
                appTeam,
                AchievementType.ALL,
                fineNames,
                threshold
        );

        assertAccomplished(calculated, 102L);
    }

    static Stream<Arguments> fanAttendanceMilestones() {
        return Stream.of(
                Arguments.of(AchievementCodes.PERMICE_NA_TRUS, 10),
                Arguments.of(AchievementCodes.ULTRUS, 30)
        );
    }

    @ParameterizedTest(name = "{0}: {1} účastí")
    @MethodSource("fanAttendanceMilestones")
    void fanMilestonesUseTheAttendanceCountFromTheirDescription(String code, int threshold) {
        when(playerAchievementRepository.findFanAttendanceMilestone(
                player.getId(), appTeam.getId(), threshold))
                .thenReturn(numbers(103L, threshold, threshold));

        PlayerAchievementDTO calculated = ReflectionTestUtils.invokeMethod(
                calculator,
                "calculateFanAttendanceMilestoneAchievement",
                player,
                achievement(code),
                appTeam,
                AchievementType.ALL,
                threshold
        );

        assertAccomplished(calculated, 103L);
    }

    static Stream<Arguments> stepMilestones() {
        return Stream.of(
                Arguments.of(AchievementCodes.OKOLO_HRADCE, 65_000L),
                Arguments.of(AchievementCodes.PRAZAK, 160_000L),
                Arguments.of(AchievementCodes.OD_SEVERU_K_JIHU, 341_000L),
                Arguments.of(AchievementCodes.OD_VYCHODU_NA_ZAPAD, 612_000L),
                Arguments.of(AchievementCodes.VSECHNY_CESTY_VEDOU_DO_RIMA, 1_600_000L),
                Arguments.of(AchievementCodes.EVROPSKY_POCHUZKAR, 7_200_000L),
                Arguments.of(AchievementCodes.CESTA_KOLEM_SVETA, 51_380_000L)
        );
    }

    @ParameterizedTest(name = "{0}: {1} kroků")
    @MethodSource("stepMilestones")
    void stepMilestonesUseTheExactDistanceFromTheirDescription(String code, long threshold) {
        when(stepAchievementCalculator.milestoneResult(player.getId(), appTeam.getId(), threshold))
                .thenReturn(Optional.of(new StepAchievementCalculator.MilestoneResult(
                        threshold, 10L, threshold * 78L / 100_000L)));

        PlayerAchievementDTO calculated = ReflectionTestUtils.invokeMethod(
                calculator,
                "calculateStepMilestoneAchievement",
                player,
                achievement(code),
                appTeam,
                threshold
        );

        assertThat(calculated.getAccomplished()).isTrue();
        verify(stepAchievementCalculator).milestoneResult(player.getId(), appTeam.getId(), threshold);
    }

    @Test
    void weddingCelebrationCountsEightBeersOrShotsTogether() {
        when(playerAchievementRepository.findFineInMatch(
                player.getId(), 104L, List.of(FineCodes.WEDDING), 1))
                .thenReturn(numbers(104L, 1, 1));
        when(playerAchievementRepository.findBeerInMatch(player.getId(), 104L))
                .thenReturn(numbers(104L, 3, 5));

        PlayerAchievementDTO calculated = invokeMatchRule(
                "calculateOZEN_SE_OZER_SEAchievementForMatch",
                AchievementCodes.OZEN_SE_OZER_SE,
                104L
        );

        assertAccomplished(calculated, 104L);
    }

    @Test
    void weddingCelebrationFailsWithOnlySevenDrinks() {
        when(playerAchievementRepository.findFineInMatch(
                player.getId(), 105L, List.of(FineCodes.WEDDING), 1))
                .thenReturn(numbers(105L, 1, 1));
        when(playerAchievementRepository.findBeerInMatch(player.getId(), 105L))
                .thenReturn(numbers(105L, 3, 4));

        assertThat(invokeMatchRule(
                "calculateOZEN_SE_OZER_SEAchievementForMatch",
                AchievementCodes.OZEN_SE_OZER_SE,
                105L
        ).getAccomplished()).isFalse();
    }

    @Test
    void koralaRequiresStrictlyMoreShotsThanBeers() {
        when(playerAchievementRepository.findBeerInMatch(player.getId(), 106L))
                .thenReturn(numbers(106L, 4, 5));
        assertAccomplished(invokeMatchRule(
                "calculateKORALAAchievementForMatch", AchievementCodes.KORALA, 106L), 106L);
    }

    @Test
    void koralaDoesNotAcceptEqualNumbers() {
        when(playerAchievementRepository.findBeerInMatch(player.getId(), 107L))
                .thenReturn(numbers(107L, 5, 5));
        assertThat(invokeMatchRule(
                "calculateKORALAAchievementForMatch", AchievementCodes.KORALA, 107L)
                .getAccomplished()).isFalse();
    }

    @Test
    void oslavenecMustDrinkMoreThanTheRestOfTheTeamCombined() {
        when(playerAchievementRepository.findPlayerAndTotalDrinksInMatch(player.getId(), 108L))
                .thenReturn(numbers(108L, 6, 11));
        assertAccomplished(invokeMatchRule(
                "calculateOSLAVENECAchievementForMatch", AchievementCodes.OSLAVENEC, 108L), 108L);
    }

    @Test
    void oslavenecDoesNotAcceptATieWithTheRestOfTheTeam() {
        when(playerAchievementRepository.findPlayerAndTotalDrinksInMatch(player.getId(), 109L))
                .thenReturn(numbers(109L, 5, 10));
        assertThat(invokeMatchRule(
                "calculateOSLAVENECAchievementForMatch", AchievementCodes.OSLAVENEC, 109L)
                .getAccomplished()).isFalse();
    }

    static Stream<Arguments> weatherRules() {
        return Stream.of(
                Arguments.of(AchievementCodes.NAVSTEVA_SAHARY, new BigDecimal("35.0"), true, 36.0),
                Arguments.of(AchievementCodes.LEDOVY_MUZ, BigDecimal.ZERO, false, -1.0)
        );
    }

    @ParameterizedTest(name = "{0}: hranice {1}, vyšší={2}")
    @MethodSource("weatherRules")
    void weatherRulesPassTheirStrictTemperatureBoundaryToTheQuery(
            String code,
            BigDecimal threshold,
            boolean higher,
            double measuredTemperature
    ) {
        IMatchIdDecimalAndNumber result = decimalNumbers(110L, measuredTemperature, null);
        when(playerAchievementRepository.findMatchAttendedByPlayerWithTemperatureThreshold(
                eq(player.getId()), eq(110L), eq(threshold), eq(higher)))
                .thenReturn(result);

        PlayerAchievementDTO calculated = ReflectionTestUtils.invokeMethod(
                calculator,
                "calculateTROPICKY_ZAPASAchievementForMatch",
                player,
                achievement(code),
                appTeam,
                AchievementType.ALL,
                110L,
                threshold,
                higher
        );

        assertAccomplished(calculated, 110L);
    }

    private PlayerAchievementDTO invokeMatchRule(String method, String code, long matchId) {
        return ReflectionTestUtils.invokeMethod(
                calculator,
                method,
                player,
                achievement(code),
                appTeam,
                AchievementType.ALL,
                matchId
        );
    }

    private void assertAccomplished(PlayerAchievementDTO calculated, long matchId) {
        assertThat(calculated).isNotNull();
        assertThat(calculated.getAccomplished()).isTrue();
        assertThat(calculated.getMatch()).isNotNull();
        assertThat(calculated.getMatch().getId()).isEqualTo(matchId);
    }

    private static IMatchIdNumberOneNumberTwo numbers(long matchId, int first, int second) {
        return new IMatchIdNumberOneNumberTwo() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Integer getFirstNumber() { return first; }
            @Override public Integer getSecondNumber() { return second; }
        };
    }

    private static IMatchIdDecimalAndNumber decimalNumbers(long matchId, double first, Integer second) {
        return new IMatchIdDecimalAndNumber() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Double getFirstNumber() { return first; }
            @Override public Integer getSecondNumber() { return second; }
        };
    }

    private static AchievementDTO achievement(String code) {
        AchievementDTO achievement = new AchievementDTO();
        achievement.setId(Math.abs(code.hashCode()));
        achievement.setCode(code);
        achievement.setName(code);
        return achievement;
    }

    private static PlayerDTO player() {
        PlayerDTO player = new PlayerDTO();
        player.setId(7L);
        player.setName("Testovací hráč");
        player.setActive(true);
        return player;
    }

    private static AppTeamEntity appTeam() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(11L);
        return appTeam;
    }
}
