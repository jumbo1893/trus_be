package com.jumbo.trus.dto.achievement;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

}
