package com.jumbo.trus.dto.achievement;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.Objects;

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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date accomplishedDate;



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

    public boolean equalsByPlayerAchievementMatchAndAccomplished(PlayerAchievementDTO o) {
        if (o == null) return false;

        return Objects.equals(this.achievement.getId(), o.achievement.getId())
                && Objects.equals(this.player.getId(), o.player.getId())
                && Objects.equals(this.match, o.match)
                && Objects.equals(this.accomplished, o.getAccomplished());
    }

}