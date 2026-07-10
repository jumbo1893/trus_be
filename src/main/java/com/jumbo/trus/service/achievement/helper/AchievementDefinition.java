package com.jumbo.trus.service.achievement.helper;

import java.util.EnumSet;
import java.util.Set;

public record AchievementDefinition(
        String code,
        AchievementScope scope,
        EnumSet<AchievementDependency> dependencies
) {

    public boolean dependsOnAny(Set<AchievementDependency> changedDependencies) {
        if (changedDependencies == null || changedDependencies.isEmpty()) {
            return true;
        }
        return dependencies.stream().anyMatch(changedDependencies::contains);
    }
}
