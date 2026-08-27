package com.jumbo.trus.service.membership;

import com.jumbo.trus.entity.membership.MembershipTier;

import java.time.Instant;

public record MembershipSnapshot(
        MembershipTier timedTier,
        MembershipTier unlimitedTier,
        long ultraMillisRemaining,
        long premiumMillisRemaining,
        Instant ultraUntil,
        Instant premiumUntil,
        long countedDrinks,
        Instant drinkCountingStartedAt,
        int drinksTowardNextPremium,
        int drinksToNextPremium
) {
}
