package com.jumbo.trus.aspect;

import com.jumbo.trus.controller.*;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.dto.beer.multi.BeerListDTO;
import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.goal.multi.GoalListDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.achievement.AchievementService;
import com.jumbo.trus.service.achievement.helper.AchievementDependency;
import com.jumbo.trus.service.achievement.helper.AchievementRecalculationContext;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.fact.RandomFactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCommitAspect {

    private final AchievementService achievementService;
    private final RandomFactService randomFactService;
    private final AppTeamService appTeamService;
    private final TaskExecutor taskExecutor;

    @AfterReturning(
            pointcut = "@annotation(com.jumbo.trus.aspect.PostCommitTask)",
            returning = "result"
    )
    public void executePostCommitTask(JoinPoint joinPoint, Object result) {
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        AchievementType achievementType =
                getAchievementTypeByClass(joinPoint.getTarget().getClass().getSimpleName());
        AchievementRecalculationContext context = buildRecalculationContext(joinPoint, result, achievementType);

        Runnable task = () -> {
            achievementService.updatePlayerAchievements(appTeam, achievementType, context);
            randomFactService.saveOrUpdateRandomFacts(appTeam);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info("Spouštím asynchronní úkol po transakci pro appTeamId={}, type={}, context={}",
                                    appTeam.getId(), achievementType, context);
                            taskExecutor.execute(task);
                        }
                    }
            );
        } else {
            log.warn("Transakce není aktivní, spouštím ihned pro appTeamId={}, type={}, context={}",
                    appTeam.getId(), achievementType, context);
            taskExecutor.execute(task);
        }
    }

    private AchievementRecalculationContext buildRecalculationContext(
            JoinPoint joinPoint,
            Object result,
            AchievementType achievementType
    ) {
        Set<Long> changedMatchIds = new HashSet<>();
        Set<Long> affectedPlayerIds = new HashSet<>();
        Set<Long> changedSeasonIds = new HashSet<>();
        Set<AchievementDependency> dependencies = dependenciesForType(achievementType);

        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();

        extractContext(result, changedMatchIds, affectedPlayerIds, changedSeasonIds);
        for (Object arg : joinPoint.getArgs()) {
            extractContext(arg, changedMatchIds, affectedPlayerIds, changedSeasonIds);
        }

        // U delete endpointů s path variablem často po smazání ztratíme původní vazbu.
        // Match/season/player mají path id přímo jako scope, u beer/goal/received_fine je to jen id položky.
        if (MatchController.class.getSimpleName().equals(controllerName)) {
            extractLongArgumentsAsMatchIds(joinPoint, changedMatchIds);
        } else if (SeasonController.class.getSimpleName().equals(controllerName)) {
            extractLongArgumentsAsSeasonIds(joinPoint, changedSeasonIds);
        } else if (PlayerController.class.getSimpleName().equals(controllerName)) {
            extractLongArgumentsAsPlayerIds(joinPoint, affectedPlayerIds);
        }

        boolean fullRecalculation = requiresSafeFullRecalculation(
                achievementType,
                controllerName,
                methodName,
                changedMatchIds,
                affectedPlayerIds,
                changedSeasonIds
        );

        if (fullRecalculation) {
            return AchievementRecalculationContext.full(dependencies);
        }

        return AchievementRecalculationContext.scoped(
                changedMatchIds,
                affectedPlayerIds,
                changedSeasonIds,
                dependencies
        );
    }

    private boolean requiresSafeFullRecalculation(
            AchievementType achievementType,
            String controllerName,
            String methodName,
            Set<Long> changedMatchIds,
            Set<Long> affectedPlayerIds,
            Set<Long> changedSeasonIds
    ) {
        if (achievementType == AchievementType.ALL) {
            return true;
        }

        // Změna definice pokuty může ovlivnit historické podmínky podle názvu pokuty.
        if (achievementType == AchievementType.FINE) {
            return true;
        }

        // U delete beer/goal/received_fine máme typicky jen id mazaného řádku, ne matchId/playerId.
        if (methodName.toLowerCase(Locale.ROOT).contains("delete")
                && (BeerController.class.getSimpleName().equals(controllerName)
                || GoalController.class.getSimpleName().equals(controllerName)
                || ReceivedFineController.class.getSimpleName().equals(controllerName))) {
            return true;
        }

        return changedMatchIds.isEmpty() && affectedPlayerIds.isEmpty() && changedSeasonIds.isEmpty();
    }

    private void extractLongArgumentsAsMatchIds(JoinPoint joinPoint, Set<Long> changedMatchIds) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long id) {
                changedMatchIds.add(id);
            }
        }
    }

    private void extractLongArgumentsAsSeasonIds(JoinPoint joinPoint, Set<Long> changedSeasonIds) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long id) {
                changedSeasonIds.add(id);
            }
        }
    }

    private void extractLongArgumentsAsPlayerIds(JoinPoint joinPoint, Set<Long> affectedPlayerIds) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long id) {
                affectedPlayerIds.add(id);
            }
        }
    }

    private void extractContext(
            Object object,
            Set<Long> changedMatchIds,
            Set<Long> affectedPlayerIds,
            Set<Long> changedSeasonIds
    ) {
        if (object == null) {
            return;
        }
        if (object instanceof Collection<?> collection) {
            collection.forEach(item -> extractContext(item, changedMatchIds, affectedPlayerIds, changedSeasonIds));
            return;
        }
        if (object instanceof MatchDTO matchDTO) {
            addPositive(changedMatchIds, matchDTO.getId());
            addPositive(changedSeasonIds, matchDTO.getSeasonId());
            if (matchDTO.getPlayerIdList() != null) {
                matchDTO.getPlayerIdList().forEach(playerId -> addPositive(affectedPlayerIds, playerId));
            }
            return;
        }
        if (object instanceof BeerDTO beerDTO) {
            addPositive(changedMatchIds, beerDTO.getMatchId());
            addPositive(affectedPlayerIds, beerDTO.getPlayerId());
            return;
        }
        if (object instanceof GoalDTO goalDTO) {
            addPositive(changedMatchIds, goalDTO.getMatchId());
            addPositive(affectedPlayerIds, goalDTO.getPlayerId());
            return;
        }
        if (object instanceof ReceivedFineDTO receivedFineDTO) {
            addPositive(changedMatchIds, receivedFineDTO.getMatchId());
            addPositive(affectedPlayerIds, receivedFineDTO.getPlayerId());
            return;
        }
        if (object instanceof BeerListDTO beerListDTO) {
            addPositive(changedMatchIds, beerListDTO.getMatchId());
            if (beerListDTO.getBeerList() != null) {
                beerListDTO.getBeerList().forEach(beer -> addPositive(affectedPlayerIds, beer.getPlayerId()));
            }
            return;
        }
        if (object instanceof GoalListDTO goalListDTO) {
            addPositive(changedMatchIds, goalListDTO.getMatchId());
            if (goalListDTO.getGoalList() != null) {
                goalListDTO.getGoalList().forEach(goal -> addPositive(affectedPlayerIds, goal.getPlayerId()));
            }
            return;
        }
        if (object instanceof ReceivedFineListDTO receivedFineListDTO) {
            addPositive(changedMatchIds, receivedFineListDTO.getMatchId());
            addPositive(affectedPlayerIds, receivedFineListDTO.getPlayerId());
            if (receivedFineListDTO.getPlayerIdList() != null) {
                receivedFineListDTO.getPlayerIdList().forEach(playerId -> addPositive(affectedPlayerIds, playerId));
            }
            if (receivedFineListDTO.getFineList() != null) {
                receivedFineListDTO.getFineList().forEach(fine -> addPositive(affectedPlayerIds, fine.getPlayerId()));
            }
            return;
        }
        if (object instanceof PlayerDTO playerDTO) {
            addPositive(affectedPlayerIds, playerDTO.getId());
            return;
        }
        if (object instanceof SeasonDTO seasonDTO) {
            addPositive(changedSeasonIds, seasonDTO.getId());
        }
    }

    private void addPositive(Set<Long> set, Long value) {
        if (value != null && value > 0) {
            set.add(value);
        }
    }

    private Set<AchievementDependency> dependenciesForType(AchievementType type) {
        if (type == AchievementType.ALL) {
            return EnumSet.allOf(AchievementDependency.class);
        }
        return switch (type) {
            case BEER -> EnumSet.of(AchievementDependency.BEER);
            case FINE -> EnumSet.of(AchievementDependency.FINE, AchievementDependency.RECEIVED_FINE);
            case RECEIVED_FINE -> EnumSet.of(AchievementDependency.RECEIVED_FINE);
            case SEASON -> EnumSet.of(AchievementDependency.SEASON, AchievementDependency.MATCH);
            case MATCH -> EnumSet.of(AchievementDependency.MATCH);
            case PLAYER -> EnumSet.of(AchievementDependency.PLAYER, AchievementDependency.MATCH);
            case GOAL -> EnumSet.of(AchievementDependency.GOAL);
            case FOOTBAR -> EnumSet.of(AchievementDependency.FOOTBAR);
            case ALL -> EnumSet.allOf(AchievementDependency.class);
        };
    }

    private AchievementType getAchievementTypeByClass(String className) {
        if (className.equals(BeerController.class.getSimpleName())) {
            return AchievementType.BEER;
        } else if (className.equals(FineController.class.getSimpleName())) {
            return AchievementType.FINE;
        } else if (className.equals(GoalController.class.getSimpleName())) {
            return AchievementType.GOAL;
        } else if (className.equals(HomeController.class.getSimpleName())) {
            return AchievementType.ALL;
        } else if (className.equals(MatchController.class.getSimpleName())) {
            return AchievementType.MATCH;
        } else if (className.equals(PlayerController.class.getSimpleName())) {
            return AchievementType.PLAYER;
        } else if (className.equals(ReceivedFineController.class.getSimpleName())) {
            return AchievementType.RECEIVED_FINE;
        } else if (className.equals(SeasonController.class.getSimpleName())) {
            return AchievementType.SEASON;
        } else if (className.equals(FootbarController.class.getSimpleName())) {
            return AchievementType.FOOTBAR;
        } else {
            return AchievementType.ALL;
        }
    }
}
