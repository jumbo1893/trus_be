package com.jumbo.trus.dto.ai;

import com.jumbo.trus.entity.ai.AiAccessTier;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class AiUsageDTO {
    private AiAccessTier tier;
    private long usedToday;
    private Integer dailyLimit;
    private Integer remainingToday;
    private boolean unlimited;
    private boolean enabled;
    private LocalDate date;
}
