package com.jumbo.trus.dto.player.stats;

import com.jumbo.trus.dto.SeasonDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStats {

    private PlayerAchievementCount playerAchievementCount;

    private PlayerBeerCount playerBeerCount;

    private PlayerFineCount playerFineCount;

    private PlayerFootbarCount playerFootbarCount;

    private PlayerGoalCount playerGoalCount;

    private SeasonDTO season;

}
