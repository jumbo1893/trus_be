package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.service.*;
import com.jumbo.trus.service.fact.RandomFact;
import com.jumbo.trus.service.pkfl.PkflMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private RandomFact randomFact;

    @Autowired
    private PkflMatchService pkflMatchService;
    @Autowired
    private ChartMaker chartMaker;


    public HomeSetup setup(Long playerId, Boolean pkflMatchesUpdateNeeded) {
        HomeSetup homeSetup = new HomeSetup();
        homeSetup.setNextBirthday(getUpcomingBirthday());
        homeSetup.setRandomFacts(randomFact.getRandomFacts());
        homeSetup.setChart(chartMaker.setupChartCoordinatesForUser(playerId));
        homeSetup.setSurroundingCharts(chartMaker.setupChartCoordinatesForSurroundingPlayers(playerId));
        homeSetup.setNextAndLastPkflMatch(pkflMatchService.getNextAndLastMatchInPkfl(pkflMatchesUpdateNeeded));

        return homeSetup;
    }

    private String getUpcomingBirthday() {
        return playerService.returnNextPlayerBirthdayFromList();
    }

}
