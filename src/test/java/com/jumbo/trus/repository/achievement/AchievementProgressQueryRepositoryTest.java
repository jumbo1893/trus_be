package com.jumbo.trus.repository.achievement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class AchievementProgressQueryRepositoryTest {

    @Autowired
    private AchievementProgressQueryRepository repository;

    @Test
    void progressQueriesAreValidPostgresSql() {
        long missingId = -1L;

        assertDoesNotThrow(() -> {
            repository.findDrinkTotals(List.of(missingId), missingId);
            repository.findFanAttendanceTotals(List.of(missingId), missingId);
            repository.sumFineCount(missingId, missingId, List.of("test"));
            repository.sumFineCountInSeason(
                    missingId, missingId, missingId, List.of("test"));
            repository.sumBeersInSeason(missingId, missingId, missingId);
            repository.findScorerProgress(missingId, missingId, missingId);
            repository.findDrinkerProgress(missingId, missingId, missingId);
        });
    }
}
