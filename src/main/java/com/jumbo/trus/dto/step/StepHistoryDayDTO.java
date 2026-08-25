package com.jumbo.trus.dto.step;

import java.time.LocalDate;

public record StepHistoryDayDTO(LocalDate date, Integer stepCount) {
}
