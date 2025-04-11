package com.jumbo.trus.dto.achievement;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class AchievementPlayerDetail {

    private List<PlayerAchievementDTO> accomplishedPlayerAchievements;

    private List<PlayerAchievementDTO> notAccomplishedPlayerAchievements;

    private Integer totalCount;

    private Float successRate;

    public AchievementPlayerDetail() {
        accomplishedPlayerAchievements = new ArrayList<>();
        notAccomplishedPlayerAchievements = new ArrayList<>();
    }
}
