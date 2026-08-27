package com.jumbo.trus.dto.membership;

import com.jumbo.trus.entity.membership.MembershipTier;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class MembershipDTO {
    private MembershipTier effectiveTier;
    private MembershipTier unlimitedTier;
    private MembershipTier timedTier;
    private Integer effectiveDailyLimit;
    private long ultraMillisRemaining;
    private long premiumMillisRemaining;
    private Instant ultraUntil;
    private Instant premiumUntil;
    private long countedDrinks;
    private Instant drinkCountingStartedAt;
    private int drinksTowardNextPremium;
    private int drinksToNextPremium;
    private int drinksPerPremiumWeek;
    private int daysPerRewardWeek;
}
