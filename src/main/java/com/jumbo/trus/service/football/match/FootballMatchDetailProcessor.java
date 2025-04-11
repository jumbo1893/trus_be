package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.dto.football.detail.FootballTableTeamDetail;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.football.helper.WinDrawLose;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.helper.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootballMatchDetailProcessor {

    private final FootballMatchProcessor footballMatchProcessor;
    private final FootballPlayerService footballPlayerService;
    private final PlayerProcessor playerProcessor;

    public FootballMatchDetail enhanceFootballMatchDetail(FootballMatchDetail footballMatchDetail, AppTeamEntity appTeam, boolean includeMutualMatches) {
        long homeTeamId = footballMatchDetail.getFootballMatch().getHomeTeam().getId();
        long awayTeamId = footballMatchDetail.getFootballMatch().getAwayTeam().getId();
        if (includeMutualMatches) {
            enhanceWithMutualMatches(footballMatchDetail, homeTeamId, awayTeamId, appTeam);
        }
        footballMatchDetail.setHomeTeamAverageBirthYear(footballPlayerService.getAverageBirthYearOfTeam(homeTeamId));
        footballMatchDetail.setAwayTeamAverageBirthYear(footballPlayerService.getAverageBirthYearOfTeam(awayTeamId));
        footballMatchDetail.setHomeTeamBestScorer(playerProcessor.findBestScorerByTeamIdAndLeagueId(homeTeamId, footballMatchDetail.getFootballMatch().getLeague().getId()));
        footballMatchDetail.setAwayTeamBestScorer(playerProcessor.findBestScorerByTeamIdAndLeagueId(awayTeamId, footballMatchDetail.getFootballMatch().getLeague().getId()));
        return footballMatchDetail;
    }

    public FootballTableTeamDetail enhanceFootballTeamDetail(FootballTableTeamDetail footballTableTeamDetail, AppTeamEntity appTeam) {
        long teamId = footballTableTeamDetail.getTableTeam().getTeamId();
        long appTeamTeamId = appTeam.getTeam().getId();
        if (teamId != appTeamTeamId) {
            footballTableTeamDetail.setMutualMatches(footballMatchProcessor.findMutualMatches(appTeamTeamId, teamId));
            Pair<String, String> aggregateScoreAndMatch = setAggregateScoreAndMatches(footballTableTeamDetail.getMutualMatches(), appTeam);
            footballTableTeamDetail.setAggregateScore(aggregateScoreAndMatch.getFirst());
            footballTableTeamDetail.setAggregateMatches(aggregateScoreAndMatch.getSecond());
        }
        else {
            footballTableTeamDetail.setMutualMatches(new ArrayList<>());
            footballTableTeamDetail.setAggregateScore("");
            footballTableTeamDetail.setAggregateMatches("");
        }
        footballTableTeamDetail.setAverageBirthYear(footballPlayerService.getAverageBirthYearOfTeam(teamId));
        footballTableTeamDetail.setBestScorer(playerProcessor.findBestScorerByTeamIdAndLeagueId(teamId, footballTableTeamDetail.getTableTeam().getLeague().getId()));
        footballTableTeamDetail.setNextMatches(footballMatchProcessor.getNextMatches(teamId));
        footballTableTeamDetail.setPastMatches(footballMatchProcessor.getPastMatchesInLeague(teamId, footballTableTeamDetail.getTableTeam().getLeague().getId()));
        return footballTableTeamDetail;
    }

    private void enhanceWithMutualMatches(FootballMatchDetail footballMatchDetail, long homeTeamId, long awayTeamId, AppTeamEntity appTeam) {
        footballMatchDetail.setMutualMatches(footballMatchProcessor.findMutualMatches(homeTeamId, awayTeamId));
        Pair<String, String> aggregateScoreAndMatch = setAggregateScoreAndMatches(footballMatchDetail.getMutualMatches(), appTeam);
        footballMatchDetail.setAggregateScore(aggregateScoreAndMatch.getFirst());
        footballMatchDetail.setAggregateMatches(aggregateScoreAndMatch.getSecond());
    }

    private Pair<String, String> setAggregateScoreAndMatches(List<FootballMatchDTO> mutualMatches, AppTeamEntity appTeam) {
        if (mutualMatches.isEmpty()) return new Pair<>(null, null);

        int[] scores = {0, 0}; // requestTeamScore, opponentScore
        int[] results = {0, 0, 0}; // wins, draws, losses

        mutualMatches.forEach(match -> {
            boolean isHomeTeam = isHomeTeamRequestedTeam(match, appTeam);
            scores[0] += isHomeTeam ? match.getHomeGoalNumber() : match.getAwayGoalNumber();
            scores[1] += isHomeTeam ? match.getAwayGoalNumber() : match.getHomeGoalNumber();

            switch (returnMatchResult(match, appTeam)) {
                case WIN -> results[0]++;
                case DRAW -> results[1]++;
                case LOSE -> results[2]++;
            }
        });

        String aggregateScore = scores[0] + ":" + scores[1];
        String aggregateMatches = results[0] + "/" + results[1] + "/" + results[2];
        return new Pair<>(aggregateScore, aggregateMatches);
    }


    private boolean isHomeTeamRequestedTeam(FootballMatchDTO match, AppTeamEntity appTeam) {
        Long requestedTeamId = appTeam.getTeam().getId();
        return match.getHomeTeam().getId().equals(requestedTeamId);
    }

    private WinDrawLose returnMatchResult(FootballMatchDTO matchDTO, AppTeamEntity appTeam) {
        int goalDifference = matchDTO.getHomeGoalNumber() - matchDTO.getAwayGoalNumber();
        if (goalDifference == 0) return WinDrawLose.DRAW;
        boolean isHomeTeam = isHomeTeamRequestedTeam(matchDTO, appTeam);
        return goalDifference > 0 == isHomeTeam ? WinDrawLose.WIN : WinDrawLose.LOSE;
    }

}
