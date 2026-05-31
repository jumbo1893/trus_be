package com.jumbo.trus.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AfterCommitExecutor {

    private final TaskExecutor taskExecutor;
    private final PlatformTransactionManager transactionManager;

    public void execute(String description, Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(() -> runSafeInNewTransaction(description, task));
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        log.debug(
                                "Přeskakuji after-commit task '{}', protože transakce byla rollbacknuta.",
                                description
                        );
                    }
                }
            });

            return;
        }

        taskExecutor.execute(() -> runSafeInNewTransaction(description, task));
    }

    private void runSafeInNewTransaction(String description, Runnable task) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            transactionTemplate.executeWithoutResult(status -> task.run());
        } catch (Exception e) {
            log.error("After-commit task '{}' selhal.", description, e);
        }
    }
}