package com.jumbo.trus.dto.step;

import java.time.LocalDate;

public record StepMatchDTO(Long matchId, String opponentName, LocalDate date) {
}
