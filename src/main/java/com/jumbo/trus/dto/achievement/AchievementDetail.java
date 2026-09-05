package com.jumbo.trus.dto.achievement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AchievementDetail {

    private AchievementDTO achievement;

    private Integer totalCount;

    private Integer accomplishedCount;

    private Float successRate;

    private PlayerAchievementDTO playerAchievement;

    private String accomplishedPlayers;

    private Set<Long> accomplishedPlayerIds;

}
