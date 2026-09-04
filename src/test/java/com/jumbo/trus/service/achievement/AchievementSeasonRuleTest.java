package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.achievement.helper.IAverageAndTwoNumbers;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import com.jumbo.trus.service.achievement.helper.ISeasonDrinkAverage;
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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementSeasonRuleTest {

    private static final long PLAYER_ID = 7L;
    private static final long TEAM_ID = 11L;
    private static final long SEASON_ID = 50L;
    private static final long LAST_MATCH_ID = 108L;

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

    private PlayerDTO player;
    private AppTeamEntity appTeam;
    private SeasonDTO season;
    private List<MatchDTO> eightMatches;

    @BeforeEach
    void setUp() {
        player = new PlayerDTO();
        player.setId(PLAYER_ID);
        player.setName("Testovací hráč");
        player.setActive(true);

        appTeam = new AppTeamEntity();
        appTeam.setId(TEAM_ID);
        season = new SeasonDTO(SEASON_ID, "2025/2026", new Date(), new Date());
        eightMatches = LongStream.rangeClosed(101L, LAST_MATCH_ID)
                .mapToObj(id -> {
                    MatchDTO match = new MatchDTO();
                    match.setId(id);
                    match.setDate(new Date(id * 1_000));
                    return match;
                })
                .toList();

        lenient().when(seasonService.getAll(any())).thenReturn(List.of(season));
        lenient().when(matchService.getAll(any())).thenReturn(eightMatches);
        lenient().when(matchService.convertMatchesToIds(any()))
                .thenAnswer(invocation -> ((List<MatchDTO>) invocation.getArgument(0)).stream()
                        .map(MatchDTO::getId)
                        .toList());
        lenient().when(matchService.getMatch(anyLong())).thenAnswer(invocation -> {
            MatchDTO match = new MatchDTO();
            match.setId(invocation.getArgument(0));
            return match;
        });
    }

    @TestFactory
    Stream<DynamicTest> seasonRulesAwardAtTheirDescribedBoundary() {
        return Stream.of(
                scenario(AchievementCodes.FOTBAL_JE_JEN_ZAMINKA, "calculateFOTBAL_JE_JEN_ZAMINKAAchievement",
                        () -> when(receivedFineService.getReceivedFineCount(
                                PLAYER_ID,
                                eightMatches.stream().map(MatchDTO::getId).toList(),
                                "Třetí poločas",
                                TEAM_ID)).thenReturn(0)),
                scenario(AchievementCodes.MECENAS, "calculateMECENASAchievement",
                        () -> when(receivedFineService.getAllDetailed(any()))
                                .thenReturn(fineStats(player, 1_000))),
                scenario(AchievementCodes.SOBEC, "calculateSOBECAchievement",
                        () -> when(goalService.getAllDetailed(any())).thenReturn(goalStats(player, 5, 0))),
                scenario(AchievementCodes.NESOBEC, "calculateNESOBECAchievement",
                        () -> when(goalService.getAllDetailed(any())).thenReturn(goalStats(player, 2, 4))),
                scenario(AchievementCodes.MIREK_DUSIN, "calculateMIREK_DUSINAchievement",
                        () -> when(receivedFineService.getAllDetailed(any()))
                                .thenReturn(fineStats(player, 100))),
                scenario(AchievementCodes.POROUCHANY_BUDIK, "calculatePOROUCHANY_BUDIKAchievement",
                        () -> when(playerAchievementRepository.findFirstMatchInSeasonWithLateArrival(PLAYER_ID, SEASON_ID))
                                .thenReturn(LAST_MATCH_ID)),
                scenario(AchievementCodes.MEDMRDKA, "calculateMEDMRDKAAchievement",
                        () -> {
                            FineDTO fine = new FineDTO(60L, "Zmínka v tisku", 100, false);
                            when(fineService.getFineByName("Zmínka v tisku", TEAM_ID)).thenReturn(fine);
                            when(receivedFineService.getAll(any())).thenReturn(List.of(
                                    new ReceivedFineDTO(1L, 1, fine, PLAYER_ID, 107L),
                                    new ReceivedFineDTO(2L, 1, fine, PLAYER_ID, LAST_MATCH_ID)
                            ));
                        }),
                scenario(AchievementCodes.PRIORITY, "calculatePRIORITYAchievement", () -> { }),
                scenario(AchievementCodes.SPORTOVEC, "calculateSPORTOVECAchievement",
                        () -> when(playerAchievementRepository.findBeersAndGoalsInSeason(PLAYER_ID, SEASON_ID))
                                .thenReturn(numbers(null, 6, 5))),
                scenario(AchievementCodes.STENE, "calculateSTENEAchievement",
                        () -> when(beerService.getAllDetailed(any())).thenReturn(beerStats(60, 0))),
                scenario(AchievementCodes.STRELEC, "calculateSTRELECAchievement",
                        () -> when(playerAchievementRepository.findStrelecInSeason(PLAYER_ID, SEASON_ID, TEAM_ID))
                                .thenReturn(numbers(null, 10, 2))),
                scenario(AchievementCodes.KDYZ_LEJU_TAK_PORADNE, "calculateKDYZ_LEJU_TAK_PORADNEAchievement",
                        () -> when(playerAchievementRepository.findKdyzLejuTakPoradneInSeason(PLAYER_ID, SEASON_ID, TEAM_ID))
                                .thenReturn(seasonDrinks(5.0, 1.0, 50, 10, 10))),
                scenario(AchievementCodes.GOLY_NE_RADEJI_PIVO, "calculateGOLY_NE_RADEJI_PIVOAchievement",
                        () -> when(playerAchievementRepository.findGolyNeRadejiPivoInSeason(PLAYER_ID, SEASON_ID, TEAM_ID))
                                .thenReturn(average(5.0, 10, 2))),
                scenario(AchievementCodes.LAZAR_NA_TRIBUNACH, "calculateLAZAR_NA_TRIBUNACHAchievement",
                        () -> when(playerAchievementRepository.findLazarNaTribune(PLAYER_ID, TEAM_ID, SEASON_ID))
                                .thenReturn(numbers(null, 3, 1))),
                scenario(AchievementCodes.ZLUTA_JE_DOBRA, "calculateZLUTA_JE_DOBRAAchievement",
                        () -> when(playerAchievementRepository.findLastMatchInSeasonWherePlayerGetsTwoFines(
                                PLAYER_ID, "Vyprazdňování při zápase", "Žlutá karta", SEASON_ID))
                                .thenReturn(numbers(LAST_MATCH_ID, 1, 1))),
                scenario(AchievementCodes.TYMOVY_HRAC, "calculateTYMOVY_HRACAchievement",
                        () -> when(playerAchievementRepository.findTeamPlayerInSeason(PLAYER_ID, SEASON_ID, TEAM_ID))
                                .thenReturn(numbers(null, 1, 5)))
        ).map(scenario -> DynamicTest.dynamicTest(scenario.code(), () -> {
            scenario.arrange().run();
            PlayerAchievementDTO result = invoke(scenario);
            assertThat(result).as(scenario.code()).isNotNull();
            assertThat(result.getAccomplished()).as(scenario.code()).isTrue();
        }));
    }

    @Test
    void fotbalJeJenZaminkaNeedsAtLeastEightMatches() {
        when(matchService.getAll(any())).thenReturn(eightMatches.subList(0, 7));

        assertThat(invoke(scenario(
                AchievementCodes.FOTBAL_JE_JEN_ZAMINKA,
                "calculateFOTBAL_JE_JEN_ZAMINKAAchievement",
                () -> { }
        )).getAccomplished()).isFalse();
    }

    @Test
    void sobecNeedsFiveGoalsAndNoAssist() {
        when(goalService.getAllDetailed(any())).thenReturn(goalStats(player, 4, 0));
        assertThat(invoke(scenario(
                AchievementCodes.SOBEC, "calculateSOBECAchievement", () -> { }
        )).getAccomplished()).isFalse();

        when(goalService.getAllDetailed(any())).thenReturn(goalStats(player, 5, 1));
        assertThat(invoke(scenario(
                AchievementCodes.SOBEC, "calculateSOBECAchievement", () -> { }
        )).getAccomplished()).isFalse();
    }

    @Test
    void nesobecNeedsFourAssistsAndAtLeastTwiceAsManyAssistsAsGoals() {
        when(goalService.getAllDetailed(any())).thenReturn(goalStats(player, 1, 3));
        assertThat(invoke(scenario(
                AchievementCodes.NESOBEC, "calculateNESOBECAchievement", () -> { }
        )).getAccomplished()).isFalse();

        when(goalService.getAllDetailed(any())).thenReturn(goalStats(player, 3, 4));
        assertThat(invoke(scenario(
                AchievementCodes.NESOBEC, "calculateNESOBECAchievement", () -> { }
        )).getAccomplished()).isFalse();
    }

    @Test
    void sportovecNeedsStrictlyMoreGoalsThanBeers() {
        when(playerAchievementRepository.findBeersAndGoalsInSeason(PLAYER_ID, SEASON_ID))
                .thenReturn(numbers(null, 5, 5));

        assertThat(invoke(scenario(
                AchievementCodes.SPORTOVEC, "calculateSPORTOVECAchievement", () -> { }
        )).getAccomplished()).isFalse();
    }

    @Test
    void steneDoesNotAcceptFiftyNineBeers() {
        when(beerService.getAllDetailed(any())).thenReturn(beerStats(59, 20));

        assertThat(invoke(scenario(
                AchievementCodes.STENE, "calculateSTENEAchievement", () -> { }
        )).getAccomplished()).isFalse();
    }

    @Test
    void mecenasAwardsEveryPlayerTiedForHighestFineAmount() {
        PlayerDTO tiedPlayer = player(8L, "Druhý mecenáš");
        when(receivedFineService.getAllDetailed(any())).thenReturn(fineStats(List.of(
                fineStatsRow(tiedPlayer, 1_000),
                fineStatsRow(player, 1_000)
        )));

        assertThat(invoke(scenario(
                AchievementCodes.MECENAS, "calculateMECENASAchievement", () -> { }
        )).getAccomplished()).isTrue();
    }

    @Test
    void mirekDusinAwardsEveryPlayerTiedForLowestFineAmount() {
        PlayerDTO mostFined = player(8L, "Nejvíce pokutovaný");
        PlayerDTO tiedPlayer = player(9L, "Druhý Mirek");
        when(receivedFineService.getAllDetailed(any())).thenReturn(fineStats(List.of(
                fineStatsRow(mostFined, 500),
                fineStatsRow(player, 100),
                fineStatsRow(tiedPlayer, 100)
        )));

        assertThat(invoke(scenario(
                AchievementCodes.MIREK_DUSIN, "calculateMIREK_DUSINAchievement", () -> { }
        )).getAccomplished()).isTrue();
    }

    private PlayerAchievementDTO invoke(Scenario scenario) {
        AchievementDTO achievement = new AchievementDTO();
        achievement.setId(Math.abs(scenario.code().hashCode()));
        achievement.setCode(scenario.code());
        achievement.setName(scenario.code());
        return ReflectionTestUtils.invokeMethod(
                calculator,
                scenario.method(),
                player,
                achievement,
                appTeam,
                AchievementType.ALL
        );
    }

    private static Scenario scenario(String code, String method, Runnable arrange) {
        return new Scenario(code, method, arrange);
    }

    private static GoalDetailedResponse goalStats(PlayerDTO player, int goals, int assists) {
        GoalDetailedDTO row = new GoalDetailedDTO(1L, goals, assists, player, null);
        return new GoalDetailedResponse(1, 1, goals, assists, List.of(row));
    }

    private static BeerDetailedResponse beerStats(int beers, int liquors) {
        return new BeerDetailedResponse(1, 1, beers, liquors, List.<BeerDetailedDTO>of(new BeerDetailedDTO()));
    }

    private static ReceivedFineDetailedResponse fineStats(PlayerDTO player, int amount) {
        return fineStats(List.of(fineStatsRow(player, amount)));
    }

    private static ReceivedFineDetailedResponse fineStats(List<ReceivedFineDetailedDTO> rows) {
        return new ReceivedFineDetailedResponse(rows.size(), 1, rows.size(),
                rows.stream().mapToInt(ReceivedFineDetailedDTO::getFineAmount).sum(), rows);
    }

    private static ReceivedFineDetailedDTO fineStatsRow(PlayerDTO player, int amount) {
        ReceivedFineDetailedDTO row = new ReceivedFineDetailedDTO();
        row.setPlayer(player);
        row.setFineAmount(amount);
        return row;
    }

    private static PlayerDTO player(long id, String name) {
        PlayerDTO player = new PlayerDTO();
        player.setId(id);
        player.setName(name);
        player.setActive(true);
        return player;
    }

    private static IMatchIdNumberOneNumberTwo numbers(Long matchId, int first, int second) {
        return new IMatchIdNumberOneNumberTwo() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Integer getFirstNumber() { return first; }
            @Override public Integer getSecondNumber() { return second; }
        };
    }

    private static ISeasonDrinkAverage seasonDrinks(
            double beerAverage, double liquorAverage, int beers, int liquors, int matches) {
        return new ISeasonDrinkAverage() {
            @Override public Long getMatchId() { return null; }
            @Override public Double getFirstNumber() { return beerAverage; }
            @Override public Double getSecondNumber() { return liquorAverage; }
            @Override public Integer getThirdNumber() { return beers; }
            @Override public Integer getFourthNumber() { return liquors; }
            @Override public Integer getFifthNumber() { return matches; }
        };
    }

    private static IAverageAndTwoNumbers average(double value, int first, int second) {
        return new IAverageAndTwoNumbers() {
            @Override public Long getMatchId() { return null; }
            @Override public Double getFirstNumber() { return value; }
            @Override public Integer getSecondNumber() { return first; }
            @Override public Integer getThirdNumber() { return second; }
        };
    }

    private record Scenario(String code, String method, Runnable arrange) {
    }
}
