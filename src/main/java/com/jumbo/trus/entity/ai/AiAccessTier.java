package com.jumbo.trus.entity.ai;

import lombok.Getter;

@Getter
public enum AiAccessTier {
    STANDARD(2),
    PREMIUM(20),
    ULTRA(null);

    private final Integer defaultDailyLimit;

    AiAccessTier(Integer defaultDailyLimit) {
        this.defaultDailyLimit = defaultDailyLimit;
    }
}
