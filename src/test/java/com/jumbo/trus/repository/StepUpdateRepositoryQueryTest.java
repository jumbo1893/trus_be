package com.jumbo.trus.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class StepUpdateRepositoryQueryTest {

    @Autowired
    private StepUpdateRepository repository;

    @Test
    void milestoneWindowQueryIsValidPostgresSql() {
        assertDoesNotThrow(() -> assertThat(
                repository.milestoneStats(-1L, -1L, 1_600_000L)
        ).isEmpty());
    }
}
