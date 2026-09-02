package com.jumbo.trus.entity.achievement;

import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "achievement")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AchievementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "achievement_seq")
    @SequenceGenerator(name = "achievement_seq", sequenceName = "achievement_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private Boolean onlyForPlayers;

    private String description;

    private String secondaryCondition;

    private Boolean manually;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @ColumnDefault("'GENERAL'")
    private AchievementCategory category = AchievementCategory.GENERAL;

    @OneToMany(mappedBy = "achievement")
    private List<PlayerAchievementEntity> playerAchievements;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "achievement_aggregate_type",
            joinColumns = @JoinColumn(name = "achievement_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_achievement_aggregate_type",
                    columnNames = {"achievement_id", "aggregate_type"}
            )
    )
    @Column(name = "aggregate_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<OutboxAggregateType> achievementTypes =
            EnumSet.noneOf(OutboxAggregateType.class);

    /**
     * Rozsah přepočítání achievementu.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_scope", nullable = false)
    private AchievementCalculationScope calculationScope;

    public AchievementEntity(
            String name,
            String code,
            Boolean onlyForPlayers,
            String description,
            String secondaryCondition,
            Boolean manually,
            Set<OutboxAggregateType> achievementTypes,
            AchievementCalculationScope calculationScope
    ) {
        this.name = name;
        this.code = code;
        this.onlyForPlayers = onlyForPlayers;
        this.description = description;
        this.secondaryCondition = secondaryCondition;
        this.manually = manually;
        this.achievementTypes = copyAchievementTypes(achievementTypes);
        this.calculationScope = calculationScope;
    }

    public AchievementEntity(
            String name,
            String code,
            Boolean onlyForPlayers,
            String description,
            Boolean manually,
            Set<OutboxAggregateType> achievementTypes,
            AchievementCalculationScope calculationScope
    ) {
        this(
                name,
                code,
                onlyForPlayers,
                description,
                null,
                manually,
                achievementTypes,
                calculationScope
        );
    }

    private static Set<OutboxAggregateType> copyAchievementTypes(
            Set<OutboxAggregateType> achievementTypes
    ) {
        if (achievementTypes == null || achievementTypes.isEmpty()) {
            return EnumSet.noneOf(OutboxAggregateType.class);
        }

        return EnumSet.copyOf(achievementTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AchievementEntity that = (AchievementEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
