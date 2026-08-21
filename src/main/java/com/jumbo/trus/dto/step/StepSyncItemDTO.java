package com.jumbo.trus.dto.step;

import com.jumbo.trus.entity.StepSource;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class StepSyncItemDTO {
    @NotNull
    private LocalDate date;

    @Min(0)
    @Max(200_000)
    private int stepCount;

    @NotNull
    private StepSource source;

    @NotBlank
    @Size(max = 64)
    private String timezone;

    @NotNull
    private OffsetDateTime measuredUntil;
}
