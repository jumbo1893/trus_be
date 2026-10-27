package com.jumbo.trus.service.fact;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.lock.LockManager;
import com.jumbo.trus.repository.DynamicTextRepository;
import com.jumbo.trus.service.SeasonService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

class RandomFactServiceTest {
    @Test
    void waitingForTeamLockDoesNotStartTransaction() throws Exception {
        SeasonService seasons = mock(SeasonService.class);
        BeerFactService beers = mock(BeerFactService.class);
        FineFactService fines = mock(FineFactService.class);
        DynamicTextRepository texts = mock(DynamicTextRepository.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(seasons.getCurrentSeason(anyBoolean(), any())).thenReturn(mock(SeasonDTO.class));
        when(beers.returnBeerFacts(any(), any(), any())).thenReturn(List.of());
        when(fines.returnFineFacts(any(), any(), any())).thenReturn(List.of());

        AppTeamEntity team = mock(AppTeamEntity.class);
        when(team.getId()).thenReturn(1L);
        LockManager locks = new LockManager();
        ReentrantLock lock = locks.getLock(1L);
        RandomFactService service = new RandomFactService(
                seasons, beers, fines, texts, locks, transactions);

        lock.lock();
        try {
            CompletableFuture<Void> work = CompletableFuture.runAsync(
                    () -> service.saveOrUpdateRandomFacts(team));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (!lock.hasQueuedThreads() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertTrue(lock.hasQueuedThreads());
            verifyNoInteractions(transactions);
            lock.unlock();
            work.get(2, TimeUnit.SECONDS);
            verify(transactions).getTransaction(any());
            verify(transactions).commit(any());
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
