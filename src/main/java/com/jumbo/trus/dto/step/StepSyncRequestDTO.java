package com.jumbo.trus.dto.step;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class StepSyncRequestDTO {
    @Valid
    @NotEmpty
    @Size(max = 31)
    private List<StepSyncItemDTO> days;
}
