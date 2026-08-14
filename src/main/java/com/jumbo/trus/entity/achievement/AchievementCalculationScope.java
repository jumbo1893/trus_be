package com.jumbo.trus.entity.achievement;

public enum AchievementCalculationScope {

    /**
     * Recalculation is triggered by a concrete changed match. The calculator may inspect
     * the minimum necessary surrounding match history for streak and milestone conditions.
     */
    MATCH,

    /** Recalculate the affected player's season-dependent result. */
    SEASON,

    /** Recalculate the affected player's full-history result. */
    ALL,

    /** Calculated outside the outbox event flow, for example manually or from login context. */
    OTHER
}
