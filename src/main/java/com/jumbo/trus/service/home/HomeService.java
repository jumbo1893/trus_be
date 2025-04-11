package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.fact.RandomFact;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final PlayerService playerService;
    private final RandomFact randomFact;
    private final ChartMaker chartMaker;
    private final FootballMatchService footballMatchService;
    private final AppTeamService appTeamService;

    public HomeSetup setup(Long userId, AppTeamEntity appTeamEntity) {
        HomeSetup homeSetup = new HomeSetup();
        homeSetup.setNextBirthday(getUpcomingBirthday(appTeamEntity.getId()));
        homeSetup.setRandomFacts(randomFact.getRandomFacts(appTeamEntity));
        homeSetup.setChart(chartMaker.setupChartCoordinatesForUser(getCurrentPlayerId(userId), appTeamEntity));
        homeSetup.setCharts(chartMaker.setupChartsCoordinates(getCurrentPlayerId(userId), appTeamEntity));
        setNextAndLastMatch(homeSetup, appTeamEntity);
        return homeSetup;
    }

    private Long getCurrentPlayerId(Long userId) {
        PlayerDTO player = appTeamService.findCurrentTeamRoleByUserId(userId).getPlayer();
        if (player == null) {
            return null;
        }
        return player.getId();
    }

    private String getUpcomingBirthday(long appTeamId) {
        return playerService.returnNextPlayerBirthdayFromList(appTeamId);
    }

    private void setNextAndLastMatch(HomeSetup homeSetup, AppTeamEntity appTeam) {
        homeSetup.setNextAndLastFootballMatch(footballMatchService.getNextAndLastFootballMatchDetail(appTeam));
    }

}
