package com.jumbo.trus.aspect;

import com.jumbo.trus.aspect.appteam.AppTeamContextHolder;
import com.jumbo.trus.controller.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.achievement.AchievementService;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCommitAspect {

    private final AchievementService achievementService;
    private final TaskExecutor taskExecutor;  // Spring-managed async executor

    @AfterReturning(pointcut = "@annotation(com.jumbo.trus.aspect.PostCommitTask)", returning = "result")
    public void executePostCommitTask(JoinPoint joinPoint, Object result) {
        AppTeamEntity appTeam = AppTeamContextHolder.getAppTeam();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Spouštím asynchronní úkol po transakci...");
                    taskExecutor.execute(() -> achievementService.updateAllPlayerAchievements(appTeam, getAchievementTypeByClass(className)));
                }
            });
        } else {
            log.warn("Transakce není aktivní, spouštím ihned...");
            taskExecutor.execute(() -> achievementService.updateAllPlayerAchievements(appTeam, getAchievementTypeByClass(className)));
        }
    }

    private AchievementType getAchievementTypeByClass(String className) {
        if (className.equals(BeerController.class.getSimpleName())) {
            return AchievementType.BEER;
        }
        else if (className.equals(FineController.class.getSimpleName())) {
            return AchievementType.FINE;
        }
        else if (className.equals(GoalController.class.getSimpleName())) {
            return AchievementType.GOAL;
        }
        else if (className.equals(HomeController.class.getSimpleName())) {
            return AchievementType.ALL;
        }else if (className.equals(MatchController.class.getSimpleName())) {
            return AchievementType.MATCH;
        }
        else if (className.equals(PlayerController.class.getSimpleName())) {
            return AchievementType.PLAYER;
        }
        else if (className.equals(ReceivedFineController.class.getSimpleName())) {
            return AchievementType.RECEIVED_FINE;
        }
        else if (className.equals(SeasonController.class.getSimpleName())) {
            return AchievementType.SEASON;
        }
        else {
            return AchievementType.ALL;
        }
    }
}
