package com.jumbo.trus.dto.footbar;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootbarSession {

    private Long id;

    private Long footbarSessionId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("start_date")
    private String startDate;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("stop_date")
    private String stopDate;

    @JsonAlias("playing_time")
    private Float playingTime;

    private String title;
    //location
    @JsonAlias("match_type")
    private String matchType;

    private String position;

    @JsonAlias("score_stars")
    private Float scoreStars;

    private Float distance;

    @JsonAlias("pass_count")
    private Integer passCount;

    @JsonAlias("shot_count")
    private Integer shotCount;

    @JsonAlias("shot_speed")
    private Float shotSpeed;

    @JsonAlias("avg_shot_speed")
    private Float avgShotSpeed;

    @JsonAlias("dribble_count")
    private Integer dribbleCount;

    @JsonAlias("time_with_ball")
    private Float timeWithBall;

    private Float activity;

    @JsonAlias("time_running")
    private Float timeRunning;

    @JsonAlias("run_count")
    private Float runCount;

    @JsonAlias("sprint_count")
    private Integer sprintCount;

    @JsonAlias("avg_sprint_speed")
    private Float avgSprintSpeed;

    @JsonAlias("sprint_speed")
    private Float sprintSpeed;

    @JsonAlias("sprint_distance")
    private Float sprintDistance;

    @JsonAlias("stop_and_go")
    private Float stopAndGo;

    private Float acceleration;
    //distance, trackerData

    private MatchDTO match;

    private PlayerDTO player;
}
