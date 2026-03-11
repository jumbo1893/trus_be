package com.jumbo.trus.dto.player;

import com.jumbo.trus.dto.achievement.AchievementPlayerDetail;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.helper.TextWithRedirect;
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

    private List<TextWithRedirect> playerStats;

    private AchievementPlayerDetail achievementPlayerDetail;

}
