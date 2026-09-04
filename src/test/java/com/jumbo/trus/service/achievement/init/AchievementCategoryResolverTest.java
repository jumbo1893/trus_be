package com.jumbo.trus.service.achievement.init;

import com.jumbo.trus.entity.achievement.AchievementCategory;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static com.jumbo.trus.service.achievement.AchievementCodes.PERMICE_NA_TRUS;
import static com.jumbo.trus.service.achievement.AchievementCodes.FLAKAC;
import static com.jumbo.trus.service.achievement.AchievementCodes.MALO_CASU_HODNE_MUZIKY;
import static com.jumbo.trus.service.achievement.AchievementCodes.TYMOVY_HRAC;
import static com.jumbo.trus.service.achievement.AchievementCodes.ZAHRANICNI_POZOROVATEL;
import static org.assertj.core.api.Assertions.assertThat;

class AchievementCategoryResolverTest {

    @Test
    void usesOneDeterministicCategoryWithFootbarAsHighestPriority() {
        assertThat(resolve("FOOTBAR_STEP", OutboxAggregateType.FOOTBAR, OutboxAggregateType.STEP))
                .isEqualTo(AchievementCategory.FOOTBAR);
        assertThat(resolve("STEP_MATCH", OutboxAggregateType.STEP, OutboxAggregateType.MATCH))
                .isEqualTo(AchievementCategory.STEPS);
        assertThat(resolve(ZAHRANICNI_POZOROVATEL, OutboxAggregateType.OTHER))
                .isEqualTo(AchievementCategory.VISITED_COUNTRIES);
        assertThat(resolve(PERMICE_NA_TRUS, OutboxAggregateType.MATCH, OutboxAggregateType.PLAYER))
                .isEqualTo(AchievementCategory.FAN);
        assertThat(resolve("BEER_FINE", OutboxAggregateType.BEER, OutboxAggregateType.RECEIVED_FINE))
                .isEqualTo(AchievementCategory.BEER);
        assertThat(resolve("FINE_GOAL", OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.GOAL))
                .isEqualTo(AchievementCategory.FINE);
        assertThat(resolve("GOAL", OutboxAggregateType.GOAL))
                .isEqualTo(AchievementCategory.MATCH);
        assertThat(resolve("OTHER", OutboxAggregateType.OTHER))
                .isEqualTo(AchievementCategory.GENERAL);
    }

    @Test
    void assignsCategoriesRequestedForNewAchievements() {
        assertThat(resolve(TYMOVY_HRAC, OutboxAggregateType.MATCH, OutboxAggregateType.GOAL))
                .isEqualTo(AchievementCategory.MATCH);
        assertThat(resolve(FLAKAC, OutboxAggregateType.RECEIVED_FINE))
                .isEqualTo(AchievementCategory.FINE);
        assertThat(resolve(MALO_CASU_HODNE_MUZIKY,
                OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.GOAL))
                .isEqualTo(AchievementCategory.FINE);
    }

    private AchievementCategory resolve(
            String code,
            OutboxAggregateType firstType,
            OutboxAggregateType... otherTypes
    ) {
        AchievementEntity achievement = new AchievementEntity();
        achievement.setCode(code);
        achievement.setAchievementTypes(EnumSet.of(firstType, otherTypes));
        return AchievementCategoryResolver.resolve(achievement);
    }
}
