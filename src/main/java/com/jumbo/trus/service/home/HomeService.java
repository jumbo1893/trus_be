package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.dto.helper.Redirect;
import com.jumbo.trus.dto.helper.RedirectDTO;
import com.jumbo.trus.dto.helper.TextWithRedirect;
import com.jumbo.trus.dto.helper.WarningType;
import com.jumbo.trus.dto.home.DashboardMatch;
import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.fact.RandomFactService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final PlayerService playerService;
    private final RandomFactService randomFactService;
    private final ChartMaker chartMaker;
    private final FootballMatchService footballMatchService;
    private final AppTeamService appTeamService;
    private final MatchService matchService;
    private final ReceivedFineService receivedFineService;


    public HomeSetup setup(Long userId, AppTeamEntity appTeamEntity) {
        HomeSetup homeSetup = new HomeSetup();
        PlayerDTO player = getCurrentPlayerId(userId);
        homeSetup.setNextBirthday(getUpcomingBirthday(appTeamEntity.getId()));
        homeSetup.setRandomFacts(randomFactService.getRandomFacts(appTeamEntity));
        homeSetup.setChart(chartMaker.setupChartCoordinatesForUser(player.getId(), appTeamEntity));
        homeSetup.setCharts(chartMaker.setupChartsCoordinates(player.getId(), appTeamEntity));
        List<FootballMatchDetail> footballMatchDetails = getNextAndLastMatch(appTeamEntity);
        homeSetup.setNextAndLastFootballMatch(footballMatchDetails);
        homeSetup.setNextMatch(getNextMatch(appTeamEntity));
        homeSetup.setLastMatch(getLastMatch(appTeamEntity, player));
        return homeSetup;
    }


    private PlayerDTO getCurrentPlayerId(Long userId) {
        return appTeamService.findCurrentTeamRoleByUserId(userId).getPlayer();
    }

    private String getUpcomingBirthday(long appTeamId) {
        return playerService.returnNextPlayerBirthdayFromList(appTeamId);
    }

    private void addTextRedirectToList(List<TextWithRedirect> matchInfoList, TextWithRedirect text) {
        if (text != null) {
            matchInfoList.add(text);
        }
    }

    private DashboardMatch getNextMatch(AppTeamEntity appTeamEntity) {
        FootballMatchDetail footballMatchDetail = footballMatchService.getNextAndLastFootballMatchDetail(appTeamEntity, true);
        DashboardMatch dashboardMatch = new DashboardMatch();
        dashboardMatch.setMatch(footballMatchDetail);
        List<TextWithRedirect> matchInfoList = new ArrayList<>();
        MatchDTO match = matchService.findMatchByFootballMatchIdOrNull(footballMatchDetail.getFootballMatch().getId(), appTeamEntity.getId());
        addTextRedirectToList(matchInfoList, getNumberOfPlayersText(footballMatchDetail, match, true));
        dashboardMatch.setMatchInfoList(matchInfoList);
        return dashboardMatch;
    }

    private DashboardMatch getLastMatch(AppTeamEntity appTeamEntity, PlayerDTO player) {
        FootballMatchDetail footballMatchDetail = footballMatchService.getNextAndLastFootballMatchDetail(appTeamEntity, false);
        DashboardMatch dashboardMatch = new DashboardMatch();
        dashboardMatch.setMatch(footballMatchDetail);
        List<TextWithRedirect> matchInfoList = new ArrayList<>();
        MatchDTO match = matchService.findMatchByFootballMatchIdOrNull(footballMatchDetail.getFootballMatch().getId(), appTeamEntity.getId());
        addTextRedirectToList(matchInfoList, getNumberOfPlayersText(footballMatchDetail, match, false));
        //addTextRedirectToList(matchInfoList, getFinesNumberText(match, appTeamEntity, player));
        dashboardMatch.setMatchInfoList(matchInfoList);
        return dashboardMatch;
    }

    private TextWithRedirect getNumberOfPlayersText(FootballMatchDetail footballMatchDetail, MatchDTO match, boolean nextMatch) {
        TextWithRedirect text = new TextWithRedirect();
        RedirectDTO redirectDTO = new RedirectDTO();
        redirectDTO.setRedirect(Redirect.MATCH_WITH_PLAYER_BOTTOMSHEET);
        redirectDTO.setFootballMatch(footballMatchDetail.getFootballMatch());
        text.setRedirect(redirectDTO);
        if (match == null || match.getPlayerIdList().isEmpty()) {
            if (nextMatch) {
                text.setText("Zatím není přihlášený žádný hráč!");
                text.setWarningType(WarningType.ERROR);
            }
            else {
                text.setText("Je potřeba doplnit hráče!");
                text.setWarningType(WarningType.ERROR);
            }
        }
        else {
            if (nextMatch) {
                text.setText("Zatím je přihlášeno " + match.getPlayerIdList().size() + " hráčů.");
                text.setWarningType(WarningType.INFO);
            }
            else {
                return null;
            }
        }
        return text;
    }

    private TextWithRedirect getFinesNumberText(MatchDTO match, AppTeamEntity appTeamEntity, PlayerDTO player) {
        if (match == null) return null;
        TextWithRedirect text = new TextWithRedirect();
        RedirectDTO redirectDTO = new RedirectDTO();
        redirectDTO.setRedirect(Redirect.PLAYER_FINE_STATS);
        redirectDTO.setMatch(match);
        text.setRedirect(redirectDTO);
        ReceivedFineDetailedResponse allPlayerResponse = receivedFineService.getAllDetailed(getAllPlayersFilter(appTeamEntity, match.getId()));
        text.setWarningType(WarningType.INFO);
        String fineText = "V zápase byly zatím uděleny pokuty v hodnotě " + allPlayerResponse.getFinesAmount() + " Kč";
        if (player != null && !player.isFan()) {
            ReceivedFineDetailedResponse playerResponse = receivedFineService.getAllDetailed(getPlayerFilter(appTeamEntity, match.getId(), player.getId()));
            fineText += ", z toho platíš " + playerResponse.getFinesAmount() + " Kč ty";
        }
        text.setText(fineText);
        return text;
    }

    private StatisticsFilter getAllPlayersFilter(AppTeamEntity appTeamEntity, Long matchId) {
        StatisticsFilter allPlayersFilter = new StatisticsFilter();
        allPlayersFilter.setMatchId(matchId);
        allPlayersFilter.setAppTeam(appTeamEntity);
        allPlayersFilter.setMatchStatsOrPlayerStats(false);
        return allPlayersFilter;
    }

    private StatisticsFilter getPlayerFilter(AppTeamEntity appTeamEntity, Long matchId, Long playerId) {
        StatisticsFilter playerFilter = new StatisticsFilter();
        playerFilter.setMatchId(matchId);
        playerFilter.setPlayerId(playerId);
        playerFilter.setAppTeam(appTeamEntity);
        playerFilter.setMatchStatsOrPlayerStats(false);
        return playerFilter;
    }

    private List<FootballMatchDetail> getNextAndLastMatch(AppTeamEntity appTeam) {
        return footballMatchService.getNextAndLastFootballMatchDetail(appTeam);
    }

}
