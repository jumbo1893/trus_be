package com.jumbo.trus.dto.achievement;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PlayerAchievementDTO {

    private long id;

    private AchievementDTO achievement;

    private PlayerDTO player;

    private MatchDTO match;

    private FootballMatchDTO footballMatch;

    private String detail;

    private Boolean accomplished;

    public PlayerAchievementDTO(AchievementDTO achievement, PlayerDTO player, MatchDTO match, String detail, Boolean accomplished) {
        this.achievement = achievement;
        this.player = player;
        this.match = match;
        this.detail = detail;
        this.accomplished = accomplished;
    }

    public PlayerAchievementDTO(AchievementDTO achievement, PlayerDTO player, FootballMatchDTO footballMatch, String detail, Boolean accomplished) {
        this.achievement = achievement;
        this.player = player;
        this.footballMatch = footballMatch;
        this.detail = detail;
        this.accomplished = accomplished;
    }

    public PlayerAchievementDTO(PlayerDTO player) {
        this.player = player;
    }

    public PlayerAchievementDTO(AchievementDTO achievement, PlayerDTO player, Boolean accomplished) {
        this.achievement = achievement;
        this.player = player;
        this.accomplished = accomplished;
    }


}