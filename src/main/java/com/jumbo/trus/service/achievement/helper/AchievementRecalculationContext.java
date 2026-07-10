package com.jumbo.trus.service.achievement.helper;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public record AchievementRecalculationContext(
        Set<Long> changedMatchIds,
        Set<Long> affectedPlayerIds,
        Set<Long> changedSeasonIds,
        Set<AchievementDependency> changedDependencies,
        boolean fullRecalculation
) {

    public AchievementRecalculationContext {
        changedMatchIds = immutableCopy(changedMatchIds);
        affectedPlayerIds = immutableCopy(affectedPlayerIds);
        changedSeasonIds = immutableCopy(changedSeasonIds);
        changedDependencies = immutableEnumCopy(changedDependencies);
    }

    public boolean hasChangedMatches() {
        return changedMatchIds != null && !changedMatchIds.isEmpty();
    }

    public boolean hasAffectedPlayers() {
        return affectedPlayerIds != null && !affectedPlayerIds.isEmpty();
    }

    public boolean hasChangedSeasons() {
        return changedSeasonIds != null && !changedSeasonIds.isEmpty();
    }

    public boolean hasChangedDependencies() {
        return changedDependencies != null && !changedDependencies.isEmpty();
    }

    public boolean shouldUseFullRecalculation() {
        return fullRecalculation;
    }

    public static AchievementRecalculationContext scoped(
            Set<Long> changedMatchIds,
            Set<Long> affectedPlayerIds,
            Set<Long> changedSeasonIds,
            Set<AchievementDependency> changedDependencies
    ) {
        return new AchievementRecalculationContext(
                changedMatchIds,
                affectedPlayerIds,
                changedSeasonIds,
                changedDependencies,
                false
        );
    }

    public static AchievementRecalculationContext full(Set<AchievementDependency> dependencies) {
        return new AchievementRecalculationContext(
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                dependencies == null ? Collections.emptySet() : dependencies,
                true
        );
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(values));
    }

    private static Set<AchievementDependency> immutableEnumCopy(Set<AchievementDependency> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }
}
