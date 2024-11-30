package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.service.*;
import com.jumbo.trus.service.fact.RandomFact;
import com.jumbo.trus.service.football.pkfl.PkflMatchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final Logger logger = LoggerFactory.getLogger(HomeService.class);
    private final PlayerService playerService;
    private final RandomFact randomFact;
    private final PkflMatchService pkflMatchService;
    private final ChartMaker chartMaker;
    private final HeaderManager headerManager;

    public HomeSetup setup(Long playerId, Boolean pkflMatchesUpdateNeeded) {
        HomeSetup homeSetup = new HomeSetup();
        logger.debug("setuju narozky");
        homeSetup.setNextBirthday(getUpcomingBirthday());
        logger.debug("setuju fakta");
        homeSetup.setRandomFacts(randomFact.getRandomFacts());
        logger.debug("setuju graf hráče");
        homeSetup.setChart(chartMaker.setupChartCoordinatesForUser(playerId));
        logger.debug("setuju graf hráčů");
        homeSetup.setCharts(chartMaker.setupChartsCoordinates(playerId));
        logger.debug("setuju příští a poslední zápas");
        homeSetup.setNextAndLastPkflMatch(pkflMatchService.getNextAndLastMatchInPkfl(pkflMatchesUpdateNeeded));
        logger.debug("hotovson");

        return homeSetup;
    }

    private String getUpcomingBirthday() {
        return playerService.returnNextPlayerBirthdayFromList();
    }

}
