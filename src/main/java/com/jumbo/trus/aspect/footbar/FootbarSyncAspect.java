package com.jumbo.trus.aspect.footbar;

import com.jumbo.trus.aspect.appteam.AppTeamContextHolder;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.activity.footbar.FootbarService;
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
public class FootbarSyncAspect {

    private final FootbarService footbarService;
    private final TaskExecutor taskExecutor;

    @AfterReturning(pointcut = "@annotation(com.jumbo.trus.aspect.footbar.FootbarSync)", returning = "result")
    public void executePostCommitTask(JoinPoint joinPoint, Object result) {
        AppTeamEntity appTeam = AppTeamContextHolder.getAppTeam();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Spouštím asynchronní úkol po transakci...");
                    taskExecutor.execute(() -> footbarService.syncSessions(appTeam));
                }
            });
        } else {
            log.warn("Transakce není aktivní, spouštím ihned...");
            taskExecutor.execute(() -> footbarService.syncSessions(appTeam));

        }
    }
}
