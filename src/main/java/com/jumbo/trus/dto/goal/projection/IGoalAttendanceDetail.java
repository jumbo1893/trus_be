package com.jumbo.trus.dto.goal.projection;

import java.util.Date;

public interface IGoalAttendanceDetail {

    Long getPlayerId();

    String getPlayerName();

    Date getPlayerBirthday();

    Boolean getFan();

    Boolean getActive();

    Long getMatchId();

    String getMatchName();

    Date getMatchDate();

    Long getSeasonId();

    Boolean getHome();

    Integer getHomeGoalNumber();

    Integer getAwayGoalNumber();

    Integer getGoalNumber();

    Integer getAssistNumber();
}
