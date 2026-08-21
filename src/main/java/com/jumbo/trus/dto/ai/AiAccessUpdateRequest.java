package com.jumbo.trus.dto.ai;

import com.jumbo.trus.entity.ai.AiAccessTier;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiAccessUpdateRequest {

    @NotNull
    private AiAccessTier tier;

    /** Volitelný vlastní limit; null použije výchozí limit tarifu. */
    @Min(1)
    private Integer dailyLimit;

    private Boolean enabled;
}
