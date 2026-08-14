package com.jumbo.trus.aspect;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.fact.RandomFactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Keeps the non-achievement post-commit work. Achievement recalculation is driven solely by outbox events.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCommitAspect {

    private final RandomFactService randomFactService;
    private final AppTeamService appTeamService;
    private final TaskExecutor taskExecutor;

    @AfterReturning(
            pointcut = "@annotation(com.jumbo.trus.aspect.PostCommitTask)",
            returning = "result"
    )
    public void executePostCommitTask(JoinPoint joinPoint, Object result) {
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        Runnable task = () -> randomFactService.saveOrUpdateRandomFacts(appTeam);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(task);
                }
            });
            return;
        }

        log.warn("Transaction is not active; running post-commit fact update immediately. appTeamId={}", appTeam.getId());
        taskExecutor.execute(task);
    }
}
