package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.achievement.helper.IGoalBeerFineMatch;
import com.jumbo.trus.service.achievement.helper.IGoalBeerMatch;
import com.jumbo.trus.service.achievement.helper.IMatchIdDecimalAndNumber;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import com.jumbo.trus.service.achievement.helper.IMatchIdThreeNumbersAndText;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Positive contract for the MATCH rules. Query semantics have focused integration tests;
 * this class makes sure each described fact is connected to the correct calculator and
 * produces an accomplished result for the affected match.
 */
@ExtendWith(MockitoExtension.class)
class AchievementMatchRuleTest {

    private static final long PLAYER_ID = 7L;
    private static final long TEAM_ID = 11L;
    private static final long FOOTBALL_TEAM_ID = 12L;
    private static final long MATCH_ID = 101L;

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

    @BeforeEach
    void setUp() {
        player = new PlayerDTO();
        player.setId(PLAYER_ID);
        player.setName("Testovací hráč");
        player.setActive(true);

        TeamEntity footballTeam = new TeamEntity();
        footballTeam.setId(FOOTBALL_TEAM_ID);
        appTeam = new AppTeamEntity();
        appTeam.setId(TEAM_ID);
        appTeam.setTeam(footballTeam);

        lenient().when(matchService.getMatch(anyLong())).thenAnswer(invocation -> {
            MatchDTO match = new MatchDTO();
            match.setId(invocation.getArgument(0));
            return match;
        });
        lenient().when(receivedFineService.getAllDetailed(any()))
                .thenReturn(new ReceivedFineDetailedResponse(0, 0, 0, 0, List.of()));
    }

    @TestFactory
    Stream<DynamicTest> repositoryBackedMatchRulesAwardTheAffectedMatch() {
        return Stream.of(
                scenario(AchievementCodes.KAZDEMU_CO_MU_PATRI, "calculateKAZDEMU_CO_MU_PATRIAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithSameGoalsAndBeers(PLAYER_ID, MATCH_ID))
                                .thenReturn(goalBeer(MATCH_ID, 2, 1, 3))),
                scenario(AchievementCodes.USPESNY_DEN, "calculateUSPESNY_DENAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithGoalYellowBeerAndLiquor(PLAYER_ID, "Žlutá karta", MATCH_ID))
                                .thenReturn(goalBeerFine(MATCH_ID, 1, 1, 1, 1))),
                scenario(AchievementCodes.DOPING, "calculateDOPINGAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithHangoverAndHattrickOrCleanSheet(
                                PLAYER_ID, "Zbytkáč či kocovina", MATCH_ID)).thenReturn(MATCH_ID)),
                scenario(AchievementCodes.ZASTRELOVANI, "calculateZASTRELOVANIAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithAtLeastXFines(
                                PLAYER_ID, MATCH_ID, "Překop", "Gól", 2, 2)).thenReturn(numbers(MATCH_ID, 2, 2))),
                scenario(AchievementCodes.JEN_NA_SKOK, "calculateJEN_NA_SKOKAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithAtLeastOneOfFinesAndXSecondFines(
                                PLAYER_ID, MATCH_ID, "Pozdní příchod do začátku", "Pozdní příchod po začátku",
                                "Pozdní příchod po 10. minutě", "Červená karta", 1)).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.DLOUHA_NOC, "calculateDLOUHA_NOCAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithAtLeastOneOfFinesAndXSecondFines(
                                PLAYER_ID, MATCH_ID, "Pozdní příchod do začátku", "Pozdní příchod po začátku",
                                "Pozdní příchod po 10. minutě", "Zbytkáč či kocovina", 1)).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.FLAKAC, "calculateFLAKACAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithAtLeastOneOfFinesAndXSecondFines(
                                PLAYER_ID, MATCH_ID, "Pozdní příchod do začátku", "Pozdní příchod po začátku",
                                "Pozdní příchod po 10. minutě", "Třetí poločas", 1)).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.MALO_CASU_HODNE_MUZIKY, "calculateMALO_CASU_HODNE_MUZIKYAchievementForMatch",
                        () -> when(playerAchievementRepository.findMaloCasuHodneMuziky(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(threeNumbers(MATCH_ID, 1, 1, 0, ""))),
                scenario(AchievementCodes.ZLUTY_HNEDY_POPLACH, "calculateZLUTY_HNEDY_POPLACHAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithAtLeastXFines(
                                PLAYER_ID, MATCH_ID, "Zbytkáč či kocovina", "Vyprazdňování při zápase", 1, 1))
                                .thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.IONTAK, "calculateIONTAKAchievementForMatch",
                        () -> when(playerAchievementRepository.findMatchWhereFineExistsAndPlayerHasBeer(
                                PLAYER_ID, "Třetí poločas", MATCH_ID)).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.PROC, "calculatePROCAchievementForMatch",
                        () -> when(playerAchievementRepository.getMatchWithAtLeastOneOfFinesAndXSecondFines(
                                PLAYER_ID, MATCH_ID, "Žlutá karta", "Červená karta", "Červená karta",
                                "Zbytkáč či kocovina", 1)).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.HLADINKA, "calculateHLADINKAAchievementForMatch",
                        () -> when(playerAchievementRepository.findMatchWhereFineExistsAndPlayerHasLiquor(
                                PLAYER_ID, "Zbytkáč či kocovina", MATCH_ID)).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.TEN_TO_PERFEKTNE_KOPE, "calculateTEN_TO_PERFEKTNE_KOPEAchievementForMatch",
                        () -> when(playerAchievementRepository.findFineInMatch(
                                PLAYER_ID, MATCH_ID, List.of("Nedal penaltu"), 1)).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.KOMPLEXNI_HRAC, "calculateKOMPLEXNI_HRACAchievementForMatch",
                        () -> when(playerAchievementRepository.findMatchWithGoalAndAssist(PLAYER_ID, MATCH_ID, TEAM_ID))
                                .thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.JARDA_KUZEL, "calculateJARDA_KUZELAchievementForMatch",
                        () -> when(playerAchievementRepository.findJardaKuzel(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 3, 1))),
                scenario(AchievementCodes.HVEZDNE_MANYRY, "calculateHVEZDNE_MANYRYAchievementForMatch",
                        () -> when(playerAchievementRepository.findBestPlayerWithFineInMatch(
                                PLAYER_ID, MATCH_ID, List.of("Pozdní příchod do začátku", "Pozdní příchod po začátku", "Pozdní příchod po 10. minutě")))
                                .thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.DAVID_BECKHAM, "calculateDAVID_BECKHAMAchievementForMatch",
                        () -> when(playerAchievementRepository.findBestPlayerWithFineInMatch(
                                PLAYER_ID, MATCH_ID, List.of("Zmínka v tisku"))).thenReturn(numbers(MATCH_ID, 1, 1))),
                scenario(AchievementCodes.ZBYTECNE_PRASE, "calculateZBYTECNE_PRASEAchievementForMatch",
                        () -> when(playerAchievementRepository.findWinningMatchWithFine(
                                PLAYER_ID, MATCH_ID, "Červená karta", FOOTBALL_TEAM_ID)).thenReturn(MATCH_ID)),
                scenario(AchievementCodes.DEN_BLBEC, "calculateDEN_BLBECAchievementForMatch",
                        () -> when(playerAchievementRepository.findMatchWherePlayerReceivedAtLeastXFines(PLAYER_ID, MATCH_ID))
                                .thenReturn(MATCH_ID)),
                scenario(AchievementCodes.ROBERTO_CARLOS, "calculateROBERTO_CARLOSAchievementForMatch",
                        () -> when(playerAchievementRepository.findRobertoCarlosInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 81, 1))),
                scenario(AchievementCodes.SPILMACHR, "calculateSPILMACHRAchievementForMatch",
                        () -> when(playerAchievementRepository.findSpilmachrInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 40, 0))),
                scenario(AchievementCodes.JA_TO_ZA_VAS_OBEHAL, "calculateJA_TO_ZA_VAS_OBEHALAchievementForMatch",
                        () -> when(playerAchievementRepository.findJaToZaVasObehalInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(decimalNumbers(MATCH_ID, 5.1, 2))),
                scenario(AchievementCodes.DOPLNENI_TEKUTIN, "calculateDOPLNENI_TEKUTINAchievementForMatch",
                        () -> when(playerAchievementRepository.findDoplneniTekutinInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(decimalNumbers(MATCH_ID, 3.0, 3))),
                scenario(AchievementCodes.CERNE_GENY, "calculateCERNE_GENYAchievementForMatch",
                        () -> when(playerAchievementRepository.findCerneGenyInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(decimalNumbers(MATCH_ID, 25.0, 1))),
                scenario(AchievementCodes.SDILENY_STRELEC, "calculateSDILENY_STRELECAchievementForMatch",
                        () -> when(playerAchievementRepository.findSdilenyStrelecInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 3, 2))),
                scenario(AchievementCodes.NESOBECKY_HRDINA, "calculateNESOBECKY_HRDINAAchievementForMatch",
                        () -> when(playerAchievementRepository.findNesobeckyHrdinaInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 3, 0))),
                scenario(AchievementCodes.MODERNI_GOLMANSKA_SKOLA, "calculateMODERNI_GOLMANSKA_SKOLAAchievementForMatch",
                        () -> when(playerAchievementRepository.findModerniGolmanskaSkolaInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 1, 90))),
                scenario(AchievementCodes.MORALNI_PODPORA, "calculateMORALNI_PODPORAAchievementForMatch",
                        () -> when(playerAchievementRepository.findMoralniPodporaInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 1, 0))),
                scenario(AchievementCodes.HATTRICK_GORDIEHO_HOWA, "calculateHATTRICK_GORDIEHO_HOWAAchievementForMatch",
                        () -> when(playerAchievementRepository.findHattrickGordiehoHowaInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(threeNumbers(MATCH_ID, 1, 1, 1, "Žlutá karta"))),
                scenario(AchievementCodes.TAHOUN, "calculateTAHOUNAAchievementForMatch",
                        () -> when(playerAchievementRepository.findTahounAtMatch(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 3, 0))),
                scenario(AchievementCodes.CERNA_PRACE, "calculateCERNA_PRACEAchievementForMatch",
                        () -> when(playerAchievementRepository.findCernaPraceInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 0, 0))),
                scenario(AchievementCodes.AUTICKO, "calculateAUTICKOAchievementForMatch",
                        () -> when(playerAchievementRepository.findAutickoInMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 1, 2))),
                scenario(AchievementCodes.ROSS_GELLER, "calculateROSS_GELLERAchievementForMatch",
                        () -> when(playerAchievementRepository.findRossGellerAtMatch(PLAYER_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 3, 3))),
                scenario(AchievementCodes.KONZISTENCE, "calculateKONZISTENCEAchievementForMatch",
                        () -> when(playerAchievementRepository.findFirstThreeConsecutiveMatchesWithGoal(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 3, 0))),
                scenario(AchievementCodes.CIRHOZA, "calculateCIRHOZAAchievementForMatch",
                        () -> when(playerAchievementRepository.findCirhozaAtMatch(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 0, 5))),
                scenario(AchievementCodes.KLUB_SRACU, "calculateKLUB_SRACUAchievementForMatch",
                        () -> when(playerAchievementRepository.findKlubSracu(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 10, 10))),
                scenario(AchievementCodes.OSAMELY_DRZAK, "calculateOSAMELY_DRZAKAchievementForMatch",
                        () -> {
                            when(playerAchievementRepository.findOsamelyDrzak(PLAYER_ID, TEAM_ID, MATCH_ID))
                                    .thenReturn(numbers(MATCH_ID, 1, 10));
                            when(playerAchievementRepository.findBeerInMatch(PLAYER_ID, MATCH_ID))
                                    .thenReturn(numbers(MATCH_ID, 1, 0));
                        }),
                scenario(AchievementCodes.VE_DVOU_SE_TO_LEPE_TAHNE, "calculateVE_DVOU_SE_TO_LEPE_TAHNEAchievementForMatch",
                        () -> when(playerAchievementRepository.findVeDvouSeToLepeTahne(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(threeNumbers(MATCH_ID, 2, 1, 0, "Spoluhráč"))),
                scenario(AchievementCodes.NASTUP_JAKO_HROM, "calculateNASTUP_JAKO_HROMAchievementForMatch",
                        () -> when(playerAchievementRepository.findNastupJakoHromGoal(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 1, 0))),
                scenario(AchievementCodes.MACHYREK, "calculateMACHYREKAchievementForMatch",
                        () -> when(playerAchievementRepository.findMachyrek(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(numbers(MATCH_ID, 10, 0))),
                scenario(AchievementCodes.DO_POCTU, "calculateDO_POCTUAchievementForMatch",
                        () -> when(playerAchievementRepository.findDoPoctu(PLAYER_ID, TEAM_ID, MATCH_ID))
                                .thenReturn(threeNumbers(MATCH_ID, 0, 0, 0, "2025/2026"))),
                scenario(AchievementCodes.ALZHEIMER, "calculateFineInMatchAchievement",
                        () -> when(playerAchievementRepository.findFineInMatch(
                                PLAYER_ID, MATCH_ID, List.of("Zapomenutí věcí", "Nekompletní výbava"), 1))
                                .thenReturn(numbers(MATCH_ID, 1, 1)),
                        List.of("Zapomenutí věcí", "Nekompletní výbava"), 1, ""),
                scenario(AchievementCodes.LEO_BERANEK, "calculateFineInMatchAchievement",
                        () -> when(playerAchievementRepository.findFineInMatch(
                                PLAYER_ID, MATCH_ID, List.of("Nový kopačky"), 1))
                                .thenReturn(numbers(MATCH_ID, 1, 1)),
                        List.of("Nový kopačky"), 1, "")
        ).map(scenario -> DynamicTest.dynamicTest(scenario.code(), () -> {
            scenario.arrange().run();
            PlayerAchievementDTO result = invoke(scenario);
            assertThat(result).as(scenario.code()).isNotNull();
            assertThat(result.getAccomplished()).as(scenario.code()).isTrue();
            assertThat(result.getMatch()).as(scenario.code()).isNotNull();
            assertThat(result.getMatch().getId()).as(scenario.code()).isEqualTo(MATCH_ID);
        }));
    }

    @Test
    void sberatelRequiresTwoAlreadyAccomplishedAchievementsOnTheSameMatch() {
        MatchEntity match = new MatchEntity();
        match.setId(MATCH_ID);
        AchievementEntity firstAchievement = new AchievementEntity();
        firstAchievement.setName("První");
        AchievementEntity secondAchievement = new AchievementEntity();
        secondAchievement.setName("Druhý");
        PlayerAchievementEntity first = new PlayerAchievementEntity();
        first.setId(1L);
        first.setAchievement(firstAchievement);
        first.setMatch(match);
        first.setAccomplished(true);
        PlayerAchievementEntity second = new PlayerAchievementEntity();
        second.setId(2L);
        second.setAchievement(secondAchievement);
        second.setMatch(match);
        second.setAccomplished(true);

        PlayerAchievementDTO firstDto = new PlayerAchievementDTO();
        firstDto.setAchievement(achievement("PRVNI", "První"));
        firstDto.setMatch(matchDto(MATCH_ID));
        PlayerAchievementDTO secondDto = new PlayerAchievementDTO();
        secondDto.setAchievement(achievement("DRUHY", "Druhý"));
        secondDto.setMatch(matchDto(MATCH_ID));

        when(playerAchievementRepository.findAccomplishedByPlayerAndMatch(PLAYER_ID, MATCH_ID))
                .thenReturn(List.of(first, second));
        when(playerAchievementMapper.toDTO(first)).thenReturn(firstDto);
        when(playerAchievementMapper.toDTO(second)).thenReturn(secondDto);

        Scenario scenario = scenario(
                AchievementCodes.SBERATEL,
                "calculateSBERATELAchievementForMatch",
                () -> { }
        );
        PlayerAchievementDTO result = invoke(scenario);

        assertThat(result.getAccomplished()).isTrue();
        assertThat(result.getMatch().getId()).isEqualTo(MATCH_ID);
        assertThat(result.getDetail()).contains("První", "Druhý");
    }

    @Test
    @Disabled("Historické pořadí ligové tabulky k datu zápasu se zatím neukládá")
    void poPoradnePraciUsesLeagueStandingAtTheTimeOfTheMatch() {
        fail("Doplnit po zavedení historie ligové tabulky k datu zápasu");
    }

    private PlayerAchievementDTO invoke(Scenario scenario) {
        AchievementDTO achievement = new AchievementDTO();
        achievement.setId(Math.abs(scenario.code().hashCode()));
        achievement.setCode(scenario.code());
        achievement.setName(scenario.code());
        if (scenario.extraArguments().length == 0) {
            return ReflectionTestUtils.invokeMethod(
                    calculator, scenario.method(), player, achievement, appTeam, AchievementType.ALL, MATCH_ID);
        }
        return ReflectionTestUtils.invokeMethod(
                calculator,
                scenario.method(),
                player,
                achievement,
                MATCH_ID,
                scenario.extraArguments()[0],
                scenario.extraArguments()[1],
                scenario.extraArguments()[2]
        );
    }

    private static Scenario scenario(String code, String method, Runnable arrange, Object... extraArguments) {
        return new Scenario(code, method, arrange, extraArguments);
    }

    private static AchievementDTO achievement(String code, String name) {
        AchievementDTO achievement = new AchievementDTO();
        achievement.setCode(code);
        achievement.setName(name);
        return achievement;
    }

    private static MatchDTO matchDto(long matchId) {
        MatchDTO match = new MatchDTO();
        match.setId(matchId);
        return match;
    }

    private static IMatchIdNumberOneNumberTwo numbers(long matchId, int first, int second) {
        return new IMatchIdNumberOneNumberTwo() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Integer getFirstNumber() { return first; }
            @Override public Integer getSecondNumber() { return second; }
        };
    }

    private static IMatchIdDecimalAndNumber decimalNumbers(long matchId, double first, int second) {
        return new IMatchIdDecimalAndNumber() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Double getFirstNumber() { return first; }
            @Override public Integer getSecondNumber() { return second; }
        };
    }

    private static IMatchIdThreeNumbersAndText threeNumbers(
            long matchId, int first, int second, int third, String text) {
        return new IMatchIdThreeNumbersAndText() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Integer getFirstNumber() { return first; }
            @Override public Integer getSecondNumber() { return second; }
            @Override public Integer getThirdNumber() { return third; }
            @Override public String getText() { return text; }
        };
    }

    private static IGoalBeerMatch goalBeer(long matchId, int beers, int liquors, int goals) {
        return new IGoalBeerMatch() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Integer getBeerNumber() { return beers; }
            @Override public Integer getLiquorNumber() { return liquors; }
            @Override public Integer getGoalNumber() { return goals; }
        };
    }

    private static IGoalBeerFineMatch goalBeerFine(
            long matchId, int beers, int liquors, int goals, int fines) {
        return new IGoalBeerFineMatch() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Integer getBeerNumber() { return beers; }
            @Override public Integer getLiquorNumber() { return liquors; }
            @Override public Integer getGoalNumber() { return goals; }
            @Override public Integer getFineNumber() { return fines; }
        };
    }

    private record Scenario(String code, String method, Runnable arrange, Object... extraArguments) {
    }
}
