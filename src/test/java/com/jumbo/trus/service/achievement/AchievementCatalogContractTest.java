package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.achievement.AchievementCalculationScope;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.init.AchievementInitializer;
import com.jumbo.trus.service.achievement.helper.AchievementType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;

/**
 * Safety net for the complete achievement catalogue. Detailed rule tests are grouped by
 * their data source, but this contract guarantees that a newly added achievement cannot
 * silently fall through without any calculation path.
 */
@ExtendWith(MockitoExtension.class)
class AchievementCatalogContractTest {

    private static final Set<String> LOCATION_ACHIEVEMENTS = Set.of(
            AchievementCodes.ZAHRANICNI_POZOROVATEL,
            AchievementCodes.DO_AFRIKY_NA_CERNOSKY,
            AchievementCodes.HEDVABNA_STEZKA,
            AchievementCodes.AMERICAN_Z_VYSOCAN,
            AchievementCodes.PO_STOPACH_DIEGA,
            AchievementCodes.TRUSI_AMUNDSEN,
            AchievementCodes.LISAK_A_MORE
    );

    private static final Set<String> AI_ACHIEVEMENTS = Set.of(
            AchievementCodes.TRUSBOT,
            AchievementCodes.AI_EXPERT
    );

    private static final Set<String> MANUAL_ACHIEVEMENTS = Set.of(
            AchievementCodes.CESTNY_JAKO_KAREL_ERBEN,
            AchievementCodes.ADA_VETVICKA
    );

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

    private List<AchievementEntity> definitions;

    @BeforeEach
    void setUp() {
        AchievementInitializer initializer = new AchievementInitializer(mock(AchievementRepository.class));
        definitions = ReflectionTestUtils.invokeMethod(initializer, "seedAchievements");

        lenient().when(seasonService.getAll(any())).thenReturn(List.<SeasonDTO>of());
        lenient().when(stepAchievementCalculator.findStrengthSaving(anyLong(), anyLong())).thenReturn(Optional.empty());
        lenient().when(stepAchievementCalculator.findWalker(anyLong(), anyLong())).thenReturn(Optional.empty());
        lenient().when(stepAchievementCalculator.milestoneResult(anyLong(), anyLong(), anyLong())).thenReturn(Optional.empty());
        lenient().when(matchService.getFirstMatchWherePlayerAttends(any())).thenReturn(null);
        lenient().when(playerAchievementRepository.getFirstMatchWithHangoverAndHattrickOrCleanSheet(anyLong(), any()))
                .thenReturn(null);
        lenient().when(playerAchievementRepository.getFirstWinningMatchWithFine(anyLong(), any(), anyLong()))
                .thenReturn(null);
        lenient().when(playerAchievementRepository.findFirstMatchWherePlayerReceivedAtLeastXFines(anyLong()))
                .thenReturn(null);
        lenient().when(receivedFineService.getAtLeastNumberOfFineInHistory(anyLong(), any(), anyInt()))
                .thenReturn(null);
    }

    @Test
    void everyDeclaredCodeHasExactlyOneDefinition() {
        Set<String> constants = Arrays.stream(AchievementCodes.class.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType() == String.class)
                .map(AchievementCatalogContractTest::readCode)
                .collect(Collectors.toSet());

        assertThat(definitions)
                .extracting(AchievementEntity::getCode)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(constants);
        assertThat(definitions).hasSize(100);
    }

    @Test
    void everyDefinitionBelongsToOneExplicitCalculationFlow() {
        Map<String, AchievementEntity> byCode = definitions.stream()
                .collect(Collectors.toMap(AchievementEntity::getCode, definition -> definition));

        assertThat(MANUAL_ACHIEVEMENTS)
                .allSatisfy(code -> assertThat(byCode.get(code).getManually()).isTrue());
        assertThat(LOCATION_ACHIEVEMENTS)
                .allSatisfy(code -> assertThat(byCode.get(code).getCalculationScope())
                        .isEqualTo(AchievementCalculationScope.OTHER));
        assertThat(AI_ACHIEVEMENTS)
                .allSatisfy(code -> assertThat(byCode.get(code).getCalculationScope())
                        .isEqualTo(AchievementCalculationScope.OTHER));

        Set<String> otherCodes = definitions.stream()
                .filter(definition -> definition.getCalculationScope() == AchievementCalculationScope.OTHER)
                .map(AchievementEntity::getCode)
                .collect(Collectors.toSet());
        assertThat(otherCodes).containsExactlyInAnyOrderElementsOf(union(LOCATION_ACHIEVEMENTS, AI_ACHIEVEMENTS, MANUAL_ACHIEVEMENTS));
    }

    @Test
    void everyAutomaticAchievementHasARegisteredCalculator() {
        Map<String, ?> registered = calculatorMap("achievementCalculators");
        Set<String> automaticCodes = definitions.stream()
                .filter(definition -> !Boolean.TRUE.equals(definition.getManually()))
                .filter(definition -> definition.getCalculationScope() != AchievementCalculationScope.OTHER)
                .map(AchievementEntity::getCode)
                .collect(Collectors.toSet());

        assertThat(registered.keySet()).containsAll(automaticCodes);
    }

    @Test
    void everyMatchAndSeasonDefinitionHasItsScopedEventCalculator() {
        Set<String> matchCodes = definitions.stream()
                .filter(definition -> !Boolean.TRUE.equals(definition.getManually()))
                .filter(definition -> definition.getCalculationScope() == AchievementCalculationScope.MATCH)
                .map(AchievementEntity::getCode)
                .collect(Collectors.toSet());
        Set<String> seasonCodes = definitions.stream()
                .filter(definition -> !Boolean.TRUE.equals(definition.getManually()))
                .filter(definition -> definition.getCalculationScope() == AchievementCalculationScope.SEASON)
                .map(AchievementEntity::getCode)
                .collect(Collectors.toSet());

        assertThat(calculatorMap("scopedAchievementCalculators").keySet()).containsAll(matchCodes);
        assertThat(calculatorMap("scopedSeasonAchievementCalculators").keySet()).containsAll(seasonCodes);
    }

    @Test
    void definitionsSubscribeToEveryFactMentionedByTheirCondition() {
        Map<String, AchievementEntity> byCode = definitions.stream()
                .collect(Collectors.toMap(AchievementEntity::getCode, definition -> definition));

        assertThat(byCode.get(AchievementCodes.KAZDEMU_CO_MU_PATRI).getAchievementTypes())
                .contains(OutboxAggregateType.BEER, OutboxAggregateType.GOAL);
        assertThat(byCode.get(AchievementCodes.PO_PORADNE_PRACI_PORADNA_OSLAVA).getAchievementTypes())
                .contains(OutboxAggregateType.BEER, OutboxAggregateType.FOOTBALL_MATCH);
        assertThat(byCode.get(AchievementCodes.DOPING).getAchievementTypes())
                .contains(OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.FOOTBALL_MATCH);
        assertThat(byCode.get(AchievementCodes.USPESNY_DEN).getAchievementTypes())
                .contains(OutboxAggregateType.BEER, OutboxAggregateType.RECEIVED_FINE,
                        OutboxAggregateType.GOAL, OutboxAggregateType.FOOTBALL_MATCH);
        assertThat(byCode.get(AchievementCodes.ROBERTO_CARLOS).getAchievementTypes())
                .contains(OutboxAggregateType.FOOTBAR, OutboxAggregateType.GOAL);
    }

    @Test
    void yellowIsGoodIsCalculatedAcrossTheAffectedSeason() {
        AchievementEntity definition = definitions.stream()
                .filter(item -> AchievementCodes.ZLUTA_JE_DOBRA.equals(item.getCode()))
                .findFirst()
                .orElseThrow();

        assertThat(definition.getCalculationScope()).isEqualTo(AchievementCalculationScope.SEASON);
        assertThat(calculatorMap("scopedSeasonAchievementCalculators").keySet())
                .contains(AchievementCodes.ZLUTA_JE_DOBRA);
    }

    @Test
    void automaticCalculatorsReturnAnExplicitFailedResultWhenFactsAreMissing() throws Exception {
        Map<String, ?> registered = calculatorMap("achievementCalculators");
        PlayerDTO player = new PlayerDTO();
        player.setId(7L);
        player.setName("Testovací hráč");
        player.setActive(true);
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(11L);

        for (AchievementEntity definition : definitions) {
            if (Boolean.TRUE.equals(definition.getManually())
                    || definition.getCalculationScope() == AchievementCalculationScope.OTHER) {
                continue;
            }

            Object function = registered.get(definition.getCode());
            PlayerAchievementDTO result = invokeCalculator(
                    function,
                    player,
                    toDto(definition),
                    appTeam
            );

            assertThat(result)
                    .as("Achievement %s musí bez splněných podmínek vrátit explicitní nesplněný výsledek", definition.getCode())
                    .isNotNull();
            assertThat(result.getAccomplished())
                    .as("Achievement %s nesmí být bez dat splněný", definition.getCode())
                    .isFalse();
        }
    }

    private Map<String, ?> calculatorMap(String fieldName) {
        return (Map<String, ?>) ReflectionTestUtils.getField(calculator, fieldName);
    }

    private PlayerAchievementDTO invokeCalculator(
            Object function,
            PlayerDTO player,
            AchievementDTO achievement,
            AppTeamEntity appTeam
    ) throws Exception {
        Method apply = Arrays.stream(function.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals("apply"))
                .findFirst()
                .orElseThrow();
        apply.setAccessible(true);
        return (PlayerAchievementDTO) apply.invoke(function, player, achievement, appTeam, AchievementType.ALL);
    }

    private AchievementDTO toDto(AchievementEntity entity) {
        AchievementDTO dto = new AchievementDTO();
        dto.setId(definitions.indexOf(entity) + 1L);
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setOnlyForPlayers(Boolean.TRUE.equals(entity.getOnlyForPlayers()));
        dto.setSecondaryCondition(entity.getSecondaryCondition());
        dto.setManually(Boolean.TRUE.equals(entity.getManually()));
        dto.setCategory(entity.getCategory());
        dto.setAchievementTypes(entity.getAchievementTypes());
        dto.setCalculationScope(entity.getCalculationScope());
        return dto;
    }

    private static String readCode(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Set<String> union(Set<String> first, Set<String> second, Set<String> third) {
        return java.util.stream.Stream.of(first, second, third)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
}
