package com.jumbo.trus.dto.step;

import com.jumbo.trus.entity.StepSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StepDailyDTO(Long id, LocalDate date, int stepCount, StepSource source,
                           String timezone, OffsetDateTime measuredUntil, Instant updatedAt) {
}
