package com.jumbo.trus.dto.step;

import java.time.LocalDate;
import java.util.List;

public record StepLeaderboardResponseDTO(
        StepPeriod period,
        LocalDate from,
        LocalDate to,
        StepMatchDTO previousMatch,
        StepMatchDTO lastMatch,
        List<StepLeaderboardDTO> entries) {
}
