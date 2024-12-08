package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.football.helper.WinDrawLose;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.helper.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootballMatchDetailProcessor {

    private final FootballMatchProcessor footballMatchProcessor;
    private final HeaderManager headerManager;
    private final FootballPlayerService footballPlayerService;
    private final PlayerProcessor playerProcessor;

    public FootballMatchDetail enhanceFootballMatchDetail(FootballMatchDetail footballMatchDetail) {
        long homeTeamId = footballMatchDetail.getFootballMatchDTO().getHomeTeam().getId();
        long awayTeamId = footballMatchDetail.getFootballMatchDTO().getAwayTeam().getId();
        footballMatchDetail.setMutualMatches(footballMatchProcessor.findMutualMatches(homeTeamId, awayTeamId));
        Pair<String, String> aggregateScoreAndMatch = setAggregateScoreAndMatches(footballMatchDetail.getMutualMatches());
        footballMatchDetail.setAggregateScore(aggregateScoreAndMatch.getFirst());
        footballMatchDetail.setAggregateMatches(aggregateScoreAndMatch.getSecond());
        footballMatchDetail.setHomeTeamAverageBirthYear(footballPlayerService.getAverageBirthYearOfTeam(homeTeamId));
        footballMatchDetail.setAwayTeamAverageBirthYear(footballPlayerService.getAverageBirthYearOfTeam(awayTeamId));
        footballMatchDetail.setHomeTeamBestScorer(playerProcessor.findBestScorerByTeamIdAndLeagueId(homeTeamId, footballMatchDetail.getFootballMatchDTO().getLeague().getId()));
        footballMatchDetail.setAwayTeamBestScorer(playerProcessor.findBestScorerByTeamIdAndLeagueId(awayTeamId, footballMatchDetail.getFootballMatchDTO().getLeague().getId()));
        return footballMatchDetail;
    }


    private Pair<String, String> setAggregateScoreAndMatches(List<FootballMatchDTO> mutualMatches) {
        if (mutualMatches.isEmpty()) return new Pair<>(null, null);

        int[] scores = {0, 0}; // requestTeamScore, opponentScore
        int[] results = {0, 0, 0}; // wins, draws, losses

        mutualMatches.forEach(match -> {
            boolean isHomeTeam = isHomeTeamRequestedTeam(match);
            scores[0] += isHomeTeam ? match.getHomeGoalNumber() : match.getAwayGoalNumber();
            scores[1] += isHomeTeam ? match.getAwayGoalNumber() : match.getHomeGoalNumber();

            switch (returnMatchResult(match)) {
                case WIN -> results[0]++;
                case DRAW -> results[1]++;
                case LOSE -> results[2]++;
            }
        });

        String aggregateScore = scores[0] + ":" + scores[1];
        String aggregateMatches = results[0] + "/" + results[1] + "/" + results[2];
        return new Pair<>(aggregateScore, aggregateMatches);
    }


    private boolean isHomeTeamRequestedTeam(FootballMatchDTO match) {
        Long requestedTeamId = headerManager.getTeamIdHeader();
        return match.getHomeTeam().getId().equals(requestedTeamId);
    }

    private WinDrawLose returnMatchResult(FootballMatchDTO matchDTO) {
        int goalDifference = matchDTO.getHomeGoalNumber() - matchDTO.getAwayGoalNumber();
        if (goalDifference == 0) return WinDrawLose.DRAW;
        boolean isHomeTeam = isHomeTeamRequestedTeam(matchDTO);
        return goalDifference > 0 == isHomeTeam ? WinDrawLose.WIN : WinDrawLose.LOSE;
    }

}
