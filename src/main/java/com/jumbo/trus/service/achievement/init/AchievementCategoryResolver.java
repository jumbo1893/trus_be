package com.jumbo.trus.service.achievement.init;

import com.jumbo.trus.entity.achievement.AchievementCategory;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;

import java.util.Set;

import static com.jumbo.trus.service.achievement.AchievementCodes.AMERICAN_Z_VYSOCAN;
import static com.jumbo.trus.service.achievement.AchievementCodes.DO_AFRIKY_NA_CERNOSKY;
import static com.jumbo.trus.service.achievement.AchievementCodes.HEDVABNA_STEZKA;
import static com.jumbo.trus.service.achievement.AchievementCodes.LISAK_A_MORE;
import static com.jumbo.trus.service.achievement.AchievementCodes.PERMICE_NA_TRUS;
import static com.jumbo.trus.service.achievement.AchievementCodes.PO_STOPACH_DIEGA;
import static com.jumbo.trus.service.achievement.AchievementCodes.TRUSI_AMUNDSEN;
import static com.jumbo.trus.service.achievement.AchievementCodes.ULTRUS;
import static com.jumbo.trus.service.achievement.AchievementCodes.ZAHRANICNI_POZOROVATEL;

final class AchievementCategoryResolver {

    private static final Set<String> COUNTRY_ACHIEVEMENTS = Set.of(
            ZAHRANICNI_POZOROVATEL,
            DO_AFRIKY_NA_CERNOSKY,
            HEDVABNA_STEZKA,
            AMERICAN_Z_VYSOCAN,
            PO_STOPACH_DIEGA,
            TRUSI_AMUNDSEN,
            LISAK_A_MORE
    );

    private static final Set<String> FAN_ACHIEVEMENTS = Set.of(
            ULTRUS,
            PERMICE_NA_TRUS
    );

    private AchievementCategoryResolver() {
    }

    static AchievementCategory resolve(AchievementEntity achievement) {
        Set<OutboxAggregateType> types = achievement.getAchievementTypes();

        if (hasType(types, OutboxAggregateType.FOOTBAR)) {
            return AchievementCategory.FOOTBAR;
        }
        if (hasType(types, OutboxAggregateType.STEP)) {
            return AchievementCategory.STEPS;
        }
        if (COUNTRY_ACHIEVEMENTS.contains(achievement.getCode())) {
            return AchievementCategory.VISITED_COUNTRIES;
        }
        if (FAN_ACHIEVEMENTS.contains(achievement.getCode())) {
            return AchievementCategory.FAN;
        }
        if (hasType(types, OutboxAggregateType.BEER)) {
            return AchievementCategory.BEER;
        }
        if (hasType(types, OutboxAggregateType.FINE)
                || hasType(types, OutboxAggregateType.RECEIVED_FINE)) {
            return AchievementCategory.FINE;
        }
        if (hasType(types, OutboxAggregateType.MATCH)
                || hasType(types, OutboxAggregateType.GOAL)
                || hasType(types, OutboxAggregateType.FOOTBALL_MATCH)) {
            return AchievementCategory.MATCH;
        }

        return AchievementCategory.GENERAL;
    }

    private static boolean hasType(
            Set<OutboxAggregateType> types,
            OutboxAggregateType type
    ) {
        return types != null && types.contains(type);
    }
}
