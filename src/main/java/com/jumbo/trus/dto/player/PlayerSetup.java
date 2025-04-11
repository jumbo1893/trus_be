package com.jumbo.trus.dto.player;

import com.jumbo.trus.dto.achievement.AchievementPlayerDetail;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.stats.FootballAllIndividualStats;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerSetup {

    private PlayerDTO player;

    private List<FootballPlayerDTO> footballPlayerList;

    private FootballPlayerDTO primaryFootballPlayer;

    @NotNull
    private FootballAllIndividualStats playerStats;

    private AchievementPlayerDetail achievementPlayerDetail;

}
