package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.stats.MatchStatsDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.stats.RefereeStatsDTO;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchStatsService {

    private final BeerService beerService;
    private final ReceivedFineService receivedFineService;
    private final GoalService goalService;
    private final MatchQueryService matchQueryService;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final MatchPlayerRatingCalculator ratingCalculator;
    private final FootballMatchRepository footballMatchRepository;


    public MatchStatsDTO getMatchStats(Long matchId, AppTeamEntity appTeam) {
        MatchStatsDTO matchStatsDTO = new MatchStatsDTO();
        StatisticsFilter statisticsFilter = new StatisticsFilter();
        statisticsFilter.setMatchStatsOrPlayerStats(false);
        statisticsFilter.setSplitPlayerFinesByFine(true);
        statisticsFilter.setMatchId(matchId);
        statisticsFilter.setAppTeam(appTeam);
        matchStatsDTO.setBeers(beerService.getAllDetailed(statisticsFilter));
        matchStatsDTO.setGoals(goalService.getAllDetailed(statisticsFilter));
        matchStatsDTO.setFines(receivedFineService.getAllDetailed(statisticsFilter));
        MatchDTO match = matchQueryService.getMatch(matchId, appTeam.getId());
        Map<Long, Long> achievementCounts = new HashMap<>();
        Long footballMatchId = match.getFootballMatch() == null
                ? null : match.getFootballMatch().getId();
        Map<Long, List<String>> achievements = new HashMap<>();
        for (var award : playerAchievementRepository.findAllAccomplishedByMatchOrFootballMatch(
                appTeam.getId(), matchId, footballMatchId)) {
            Long playerId = award.getPlayer().getId();
            achievementCounts.merge(playerId, 1L, Long::sum);
            achievements.computeIfAbsent(playerId, ignored -> new ArrayList<>())
                    .add(award.getAchievement().getName());
        }
        matchStatsDTO.setPlayerAchievements(achievements);
        matchStatsDTO.setPlayerRatings(ratingCalculator.calculate(match, matchStatsDTO, achievementCounts));
        if (match.getFootballMatch() != null
                && match.getFootballMatch().getReferee() != null
                && !match.getFootballMatch().getReferee().isBlank()) {
            String referee = match.getFootballMatch().getReferee().trim();
            Long teamId = match.isHome()
                    ? match.getFootballMatch().getHomeTeam().getId()
                    : match.getFootballMatch().getAwayTeam().getId();
            var totalRows = footballMatchRepository.refereeTeamTotals(teamId, referee);
            Object[] totals = totalRows.isEmpty() ? new Object[]{0L, 0L, 0L, 0L} : totalRows.get(0);
            long matches = ((Number) totals[0]).longValue();
            long wins = ((Number) totals[1]).longValue();
            long yellow = ((Number) totals[2]).longValue();
            long red = ((Number) totals[3]).longValue();
            var stars = footballMatchRepository.refereeMostStarredPlayer(teamId, referee);
            String playerName = stars.isEmpty() ? null : (String) stars.get(0)[0];
            long starCount = stars.isEmpty() ? 0 : ((Number) stars.get(0)[1]).longValue();
            matchStatsDTO.setRefereeStats(new RefereeStatsDTO(
                    referee, matches, matches == 0 ? 0 : wins * 100.0 / matches,
                    playerName, starCount,
                    matches == 0 ? 0 : yellow * 1.0 / matches,
                    matches == 0 ? 0 : red * 1.0 / matches));
        }
        return matchStatsDTO;
    }

}
