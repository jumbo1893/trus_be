package com.jumbo.trus.dto.achievement;

import com.jumbo.trus.entity.achievement.AchievementCalculationScope;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AchievementDTO {

    private long id;

    private String name;

    private String code;

    private String description;

    private boolean onlyForPlayers;

    private String secondaryCondition;

    private boolean manually;

    private Set<OutboxAggregateType> achievementTypes;

    private AchievementCalculationScope calculationScope;

    private Float teamSuccessRate;

    private AchievementRarity rarity;
}
