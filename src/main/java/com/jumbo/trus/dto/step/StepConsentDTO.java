package com.jumbo.trus.dto.step;

import jakarta.validation.constraints.NotNull;

public record StepConsentDTO(@NotNull Boolean enabled) {
}
