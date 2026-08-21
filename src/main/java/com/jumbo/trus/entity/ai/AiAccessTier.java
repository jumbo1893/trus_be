package com.jumbo.trus.entity.ai;

import lombok.Getter;

@Getter
public enum AiAccessTier {
    STANDARD(2, 6),
    PREMIUM(20, 10),
    ULTRA(null, 14);

    private final Integer defaultDailyLimit;
    private final int maxToolRounds;

    AiAccessTier(Integer defaultDailyLimit, int maxToolRounds) {
        this.defaultDailyLimit = defaultDailyLimit;
        this.maxToolRounds = maxToolRounds;
    }
}
