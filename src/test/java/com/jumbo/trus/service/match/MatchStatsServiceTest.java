package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchStatsServiceTest {

    @Test
    void refereeAggregateRowIsReadAsFourColumns() {
        BeerService beers = mock(BeerService.class);
        ReceivedFineService fines = mock(ReceivedFineService.class);
        GoalService goals = mock(GoalService.class);
        MatchQueryService matches = mock(MatchQueryService.class);
        PlayerAchievementRepository achievements = mock(PlayerAchievementRepository.class);
        MatchPlayerRatingCalculator ratings = mock(MatchPlayerRatingCalculator.class);
        FootballMatchRepository footballMatches = mock(FootballMatchRepository.class);
        MatchStatsService service = new MatchStatsService(
                beers, fines, goals, matches, achievements, ratings, footballMatches);

        AppTeamEntity team = new AppTeamEntity();
        team.setId(3L);
        TeamDTO homeTeam = new TeamDTO();
        homeTeam.setId(9L);
        FootballMatchDTO footballMatch = new FootballMatchDTO();
        footballMatch.setReferee("Novák");
        footballMatch.setHomeTeam(homeTeam);
        footballMatch.setId(12L);
        MatchDTO match = new MatchDTO();
        match.setId(7L);
        match.setHome(true);
        match.setFootballMatch(footballMatch);
        match.setPlayerIdList(List.of());

        when(matches.getMatch(7L, 3L)).thenReturn(match);
        when(achievements.findAllAccomplishedByMatchOrFootballMatch(3L, 7L, 12L))
                .thenReturn(List.of());
        when(ratings.calculate(any(), any(), any())).thenReturn(Map.of());
        when(footballMatches.refereeTeamTotals(9L, "Novák"))
                .thenReturn(List.<Object[]>of(new Object[]{4L, 2L, 8L, 1L}));
        when(footballMatches.refereeMostStarredPlayer(9L, "Novák"))
                .thenReturn(List.<Object[]>of(new Object[]{"Hráč", 3L}));

        var result = service.getMatchStats(7L, team).getRefereeStats();

        assertThat(result.getMatchCount()).isEqualTo(4);
        assertThat(result.getWinPercentage()).isEqualTo(50.0);
        assertThat(result.getYellowCardsPerMatch()).isEqualTo(2.0);
        assertThat(result.getRedCardsPerMatch()).isEqualTo(0.25);
        assertThat(result.getMostStarredPlayer()).isEqualTo("Hráč");
        assertThat(result.getMostStarredCount()).isEqualTo(3);
    }
}
