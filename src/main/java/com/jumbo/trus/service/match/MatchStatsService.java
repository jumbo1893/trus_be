package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.stats.MatchStatsDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.GoalService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchStatsService {

    private final BeerService beerService;
    private final ReceivedFineService receivedFineService;
    private final GoalService goalService;


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
        return matchStatsDTO;
    }

}
