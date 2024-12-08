package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.service.*;
import com.jumbo.trus.service.fact.RandomFact;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.pkfl.PkflMatchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final PlayerService playerService;
    private final RandomFact randomFact;
    private final PkflMatchService pkflMatchService;
    private final ChartMaker chartMaker;
    private final HeaderManager headerManager;
    private final FootballMatchService footballMatchService;

    public HomeSetup setup(Long playerId, Boolean pkflMatchesUpdateNeeded) {
        Long teamId = headerManager.getTeamIdHeader();
        HomeSetup homeSetup = new HomeSetup();
        homeSetup.setNextBirthday(getUpcomingBirthday());
        homeSetup.setRandomFacts(randomFact.getRandomFacts());
        homeSetup.setChart(chartMaker.setupChartCoordinatesForUser(playerId));
        homeSetup.setCharts(chartMaker.setupChartsCoordinates(playerId));
        setNextAndLastMatch(homeSetup, teamId, pkflMatchesUpdateNeeded);
        return homeSetup;
    }

    private String getUpcomingBirthday() {
        return playerService.returnNextPlayerBirthdayFromList();
    }

    private void setNextAndLastMatch(HomeSetup homeSetup, Long teamId, Boolean pkflMatchesUpdateNeeded) {
        if (teamId == null) {
            homeSetup.setNextAndLastPkflMatch(pkflMatchService.getNextAndLastMatchInPkfl(pkflMatchesUpdateNeeded));
        }
        else {
            homeSetup.setNextAndLastFootballMatch(footballMatchService.getNextAndLastFootballMatch(teamId));
        }
    }

}
