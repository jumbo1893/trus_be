package com.jumbo.trus.dto.player.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerAchievementCount {

    private int totalAchievements;

    private int accomplishedAchievements;

}
