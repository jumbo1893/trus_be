package com.jumbo.trus.dto.ai;

import com.jumbo.trus.entity.ai.AiAccessTier;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiAccessDTO {
    private Long userId;
    private String userName;
    private String userMail;
    private AiAccessTier tier;
    private Integer dailyLimit;
    private boolean enabled;
}
