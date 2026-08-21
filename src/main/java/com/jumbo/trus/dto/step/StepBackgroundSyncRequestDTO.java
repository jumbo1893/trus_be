package com.jumbo.trus.dto.step;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record StepBackgroundSyncRequestDTO(
        @NotBlank @Email String mail,
        @NotBlank String password,
        @NotNull @Positive Long appTeamId,
        @NotNull Boolean permissionGranted,
        @Valid @NotNull @Size(max = 31) List<StepSyncItemDTO> days
) {
}
