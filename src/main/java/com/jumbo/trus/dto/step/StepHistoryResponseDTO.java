package com.jumbo.trus.dto.step;

import java.time.LocalDate;
import java.util.List;

public record StepHistoryResponseDTO(
        Long userId,
        String userName,
        LocalDate from,
        LocalDate to,
        List<StepHistoryDayDTO> days
) {
}
