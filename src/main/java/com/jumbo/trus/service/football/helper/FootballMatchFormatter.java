package com.jumbo.trus.service.football.helper;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.service.helper.DateFormatter;

public class FootballMatchFormatter {

    public static String toStringBasic(FootballMatchDTO match) {
        return String.format("%s vs %s", match.getHomeTeam().getName(), match.getAwayTeam().getName());
    }

    public static String toStringWithDateAndStadium(FootballMatchDTO match) {
        return String.format("%s vs %s v čase %s na hřišti %s",
                match.getHomeTeam().getName(), 
                match.getAwayTeam().getName(),
                DateFormatter.formatDateForFrontend(match.getDate()),
                match.getStadium());
    }

    public static String toStringWithResult(FootballMatchDTO match) {
        return String.format("Výsledek %s vs %s je %s:%s",
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeGoalNumber(),
                match.getAwayTeam());
    }

    public static String toStringWithResultAndRefereeComment(FootballMatchDTO match) {
        return String.format("Zápas %s vs %s %s:%s, komentář: %s",
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeGoalNumber(),
                match.getAwayTeam(),
                match.getRefereeComment());
    }
}
