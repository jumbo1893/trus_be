package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.football.FootballMatchDTO;
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
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.fact.RandomFactService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.player.PlayerAchievementService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
    private final StatsBoardDataService statsBoardDataService;
    private final PlayerAchievementService playerAchievementService;

    public HomeSetup setup(Long userId, AppTeamEntity appTeamEntity) {
        long setupStart = System.currentTimeMillis();
        Long appTeamId = appTeamEntity != null ? appTeamEntity.getId() : null;

        log.info("[HOME_SETUP] START userId={}, appTeamId={}", userId, appTeamId);

        try {
            HomeSetup homeSetup = new HomeSetup();

            PlayerDTO player = logDuration("getCurrentPlayerId", userId, appTeamId,
                    () -> getCurrentPlayerId(userId));

            log.info("[HOME_SETUP] currentPlayer userId={}, appTeamId={}, playerId={}, playerName={}, fan={}",
                    userId,
                    appTeamId,
                    player != null ? player.getId() : null,
                    player != null ? player.getName() : null,
                    player != null ? player.isFan() : null
            );

            homeSetup.setNextBirthday(logDuration("getUpcomingBirthday", userId, appTeamId,
                    () -> getUpcomingBirthday(appTeamEntity.getId())));

            homeSetup.setRandomFacts(logDuration("randomFactService.getRandomFacts", userId, appTeamId,
                    () -> randomFactService.getRandomFacts(appTeamEntity)));

            homeSetup.setChart(logDuration("chartMaker.setupChartCoordinatesForUser", userId, appTeamId,
                    () -> chartMaker.setupChartCoordinatesForUser(player.getId(), appTeamEntity)));

            homeSetup.setCharts(logDuration("chartMaker.setupChartsCoordinates", userId, appTeamId,
                    () -> chartMaker.setupChartsCoordinates(player.getId(), appTeamEntity)));

            List<FootballMatchDetail> footballMatchDetails = logDuration("getNextAndLastMatch", userId, appTeamId,
                    () -> getNextAndLastMatch(appTeamEntity));
            homeSetup.setNextAndLastFootballMatch(footballMatchDetails);

            homeSetup.setNextMatch(logDuration("getNextMatch", userId, appTeamId,
                    () -> getNextMatch(appTeamEntity)));

            homeSetup.setLastMatch(logDuration("getLastMatch", userId, appTeamId,
                    () -> getLastMatch(appTeamEntity, player)));

            homeSetup.setStatsBoards(logDuration("statsBoardDataService.getStatsBoardDataList", userId, appTeamId,
                    () -> statsBoardDataService.getStatsBoardDataList(appTeamEntity)));

            return homeSetup;
        } finally {
            long totalMs = System.currentTimeMillis() - setupStart;
            log.info("[HOME_SETUP] END userId={}, appTeamId={}, totalMs={}", userId, appTeamId, totalMs);
        }
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
        Long appTeamId = appTeamEntity != null ? appTeamEntity.getId() : null;

        FootballMatchDetail footballMatchDetail = logDuration(
                "getNextMatch.footballMatchService.getNextAndLastFootballMatchDetail",
                null,
                appTeamId,
                () -> footballMatchService.getNextAndLastFootballMatchDetail(appTeamEntity, true)
        );

        MatchDTO match = logDuration(
                "getNextMatch.matchService.findMatchByFootballMatchIdOrNull",
                null,
                appTeamId,
                () -> matchService.findMatchByFootballMatchIdOrNull(
                        footballMatchDetail.getFootballMatch().getId(),
                        appTeamEntity.getId()
                )
        );

        logDashboardMatchData("getNextMatch", footballMatchDetail, match);

        if (match != null && match.getHomeGoalNumber() != null && match.getAwayGoalNumber() != null) {
            footballMatchDetail.getFootballMatch().setHomeGoalNumber(match.getHomeGoalNumber());
            footballMatchDetail.getFootballMatch().setAwayGoalNumber(match.getAwayGoalNumber());
        }

        DashboardMatch dashboardMatch = new DashboardMatch();
        dashboardMatch.setMatch(footballMatchDetail);

        List<TextWithRedirect> matchInfoList = new ArrayList<>();
        addTextRedirectToList(matchInfoList, logDuration(
                "getNextMatch.getNumberOfPlayersText",
                null,
                appTeamId,
                () -> getNumberOfPlayersText(footballMatchDetail, match, true)
        ));

        dashboardMatch.setMatchInfoList(matchInfoList);
        return dashboardMatch;
    }

    private DashboardMatch getLastMatch(AppTeamEntity appTeamEntity, PlayerDTO player) {
        Long appTeamId = appTeamEntity != null ? appTeamEntity.getId() : null;
        Long playerId = player != null ? player.getId() : null;

        FootballMatchDetail footballMatchDetail = logDuration(
                "getLastMatch.footballMatchService.getNextAndLastFootballMatchDetail",
                playerId,
                appTeamId,
                () -> footballMatchService.getNextAndLastFootballMatchDetail(appTeamEntity, false)
        );

        MatchDTO match = logDuration(
                "getLastMatch.matchService.findMatchByFootballMatchIdOrNull",
                playerId,
                appTeamId,
                () -> {
                    assert appTeamEntity != null;
                    return matchService.findMatchByFootballMatchIdOrNull(
                            footballMatchDetail.getFootballMatch().getId(),
                            appTeamEntity.getId()
                    );
                }
        );

        logDashboardMatchData("getLastMatch", footballMatchDetail, match);

        if (match != null && match.getHomeGoalNumber() != null && match.getAwayGoalNumber() != null) {
            footballMatchDetail.getFootballMatch().setHomeGoalNumber(match.getHomeGoalNumber());
            footballMatchDetail.getFootballMatch().setAwayGoalNumber(match.getAwayGoalNumber());
        }

        DashboardMatch dashboardMatch = new DashboardMatch();
        dashboardMatch.setMatch(footballMatchDetail);

        List<TextWithRedirect> matchInfoList = new ArrayList<>();

        addTextRedirectToList(matchInfoList, logDuration(
                "getLastMatch.getNumberOfPlayersText",
                playerId,
                appTeamId,
                () -> getNumberOfPlayersText(footballMatchDetail, match, false)
        ));

        addTextRedirectToList(matchInfoList, logDuration(
                "getLastMatch.getFinesNumberText",
                playerId,
                appTeamId,
                () -> getFinesNumberText(match, appTeamEntity, player)
        ));

        List<TextWithRedirect> achievementsTexts = logDuration(
                "getLastMatch.getAccomplishedAchievements",
                playerId,
                appTeamId,
                () -> getAccomplishedAchievements(match, footballMatchDetail.getFootballMatch(), appTeamEntity)
        );

        for (TextWithRedirect achievementsText : achievementsTexts) {
            addTextRedirectToList(matchInfoList, achievementsText);
        }

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
            } else {
                text.setText("Je potřeba doplnit hráče!");
                text.setWarningType(WarningType.ERROR);
            }
        } else {
            if (nextMatch) {
                text.setText("Zatím je přihlášeno " + match.getPlayerIdList().size() + " hráčů.");
                text.setWarningType(WarningType.INFO);
            } else {
                return null;
            }
        }

        return text;
    }

    private TextWithRedirect getFinesNumberText(MatchDTO match, AppTeamEntity appTeamEntity, PlayerDTO player) {
        if (match == null) {
            log.info("[HOME_SETUP] getFinesNumberText skipped because match is null");
            return null;
        }

        Long appTeamId = appTeamEntity != null ? appTeamEntity.getId() : null;
        Long playerId = player != null ? player.getId() : null;

        TextWithRedirect text = new TextWithRedirect();

        RedirectDTO redirectDTO = new RedirectDTO();
        redirectDTO.setRedirect(Redirect.PLAYER_FINE_STATS);
        redirectDTO.setMatch(match);

        text.setRedirect(redirectDTO);

        ReceivedFineDetailedResponse allPlayerResponse = logDuration(
                "getFinesNumberText.receivedFineService.getAllDetailed.allPlayers",
                playerId,
                appTeamId,
                () -> receivedFineService.getAllDetailed(getAllPlayersFilter(appTeamEntity, match.getId()))
        );

        text.setWarningType(WarningType.INFO);

        String fineText = "V zápase byly zatím uděleny pokuty v hodnotě " + allPlayerResponse.getFinesAmount() + " Kč";

        if (player != null && !player.isFan()) {
            ReceivedFineDetailedResponse playerResponse = logDuration(
                    "getFinesNumberText.receivedFineService.getAllDetailed.currentPlayer",
                    playerId,
                    appTeamId,
                    () -> receivedFineService.getAllDetailed(getPlayerFilter(appTeamEntity, match.getId(), player.getId()))
            );

            fineText += ", z toho platíš " + playerResponse.getFinesAmount() + " Kč ty";
        }

        text.setText(fineText);
        return text;
    }

    private List<TextWithRedirect> getAccomplishedAchievements(MatchDTO match, FootballMatchDTO footballMatchDTO, AppTeamEntity appTeamEntity) {
        if (match == null && footballMatchDTO == null) {
            log.info("[HOME_SETUP] getAccomplishedAchievements skipped because match and footballMatchDTO are null");
            return Collections.emptyList();
        }

        Long matchId = Optional.ofNullable(match)
                .map(MatchDTO::getId)
                .orElse(null);

        Long footballMatchId = Optional.ofNullable(footballMatchDTO)
                .map(FootballMatchDTO::getId)
                .orElse(null);

        Long appTeamId = appTeamEntity != null ? appTeamEntity.getId() : null;

        log.info("[HOME_SETUP] getAccomplishedAchievements input appTeamId={}, matchId={}, footballMatchId={}",
                appTeamId, matchId, footballMatchId);

        assert appTeamEntity != null;
        List<PlayerAchievementDTO> achievements = playerAchievementService.getAllAccomplishedAchievementsByMatch(
                appTeamEntity.getId(),
                matchId,
                footballMatchId
        );

        log.info("[HOME_SETUP] getAccomplishedAchievements result appTeamId={}, matchId={}, footballMatchId={}, achievementsCount={}",
                appTeamId,
                matchId,
                footballMatchId,
                achievements != null ? achievements.size() : null
        );

        if (achievements == null || achievements.isEmpty()) {
            return Collections.emptyList();
        }

        List<TextWithRedirect> textWithRedirects = new ArrayList<>();

        for (PlayerAchievementDTO achievement : achievements) {
            TextWithRedirect text = new TextWithRedirect();

            RedirectDTO redirectDTO = new RedirectDTO();
            redirectDTO.setRedirect(Redirect.ACHIEVEMENTS);

            text.setRedirect(redirectDTO);
            text.setWarningType(WarningType.INFO);
            text.setText("V zápase byl získaný achievement "
                    + achievement.getAchievement().getName()
                    + " hráčem "
                    + achievement.getPlayer().getName());

            textWithRedirects.add(text);
        }

        return textWithRedirects;
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

    private <T> T logDuration(String stepName, Long userIdOrPlayerId, Long appTeamId, Supplier<T> supplier) {
        long start = System.currentTimeMillis();

        log.info("[HOME_SETUP] STEP_START step={}, userIdOrPlayerId={}, appTeamId={}",
                stepName,
                userIdOrPlayerId,
                appTeamId);

        try {
            T result = supplier.get();

            long durationMs = System.currentTimeMillis() - start;

            log.info("[HOME_SETUP] STEP_END step={}, userIdOrPlayerId={}, appTeamId={}, durationMs={}, result={}",
                    stepName,
                    userIdOrPlayerId,
                    appTeamId,
                    durationMs,
                    summarizeResult(result));

            return result;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - start;

            log.error("[HOME_SETUP] STEP_FAILED step={}, userIdOrPlayerId={}, appTeamId={}, durationMs={}",
                    stepName,
                    userIdOrPlayerId,
                    appTeamId,
                    durationMs,
                    e);

            throw e;
        }
    }

    private String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }

        if (result instanceof List<?> list) {
            return "List(size=" + list.size() + ")";
        }

        if (result instanceof DashboardMatch dashboardMatch) {
            FootballMatchDetail matchDetail = dashboardMatch.getMatch();

            Long footballMatchId = Optional.ofNullable(matchDetail)
                    .map(FootballMatchDetail::getFootballMatch)
                    .map(FootballMatchDTO::getId)
                    .orElse(null);

            int infoSize = dashboardMatch.getMatchInfoList() != null
                    ? dashboardMatch.getMatchInfoList().size()
                    : 0;

            return "DashboardMatch(footballMatchId=" + footballMatchId + ", infoSize=" + infoSize + ")";
        }

        if (result instanceof MatchDTO match) {
            int playerCount = match.getPlayerIdList().size();
            return "MatchDTO(id=" + match.getId() + ", footballMatchId=" + match.getFootballMatch().getId() + ", playerCount=" + playerCount + ")";
        }

        if (result instanceof FootballMatchDetail footballMatchDetail) {
            Long footballMatchId = Optional.of(footballMatchDetail.getFootballMatch())
                    .map(FootballMatchDTO::getId)
                    .orElse(null);

            return "FootballMatchDetail(footballMatchId=" + footballMatchId + ")";
        }

        if (result instanceof ReceivedFineDetailedResponse response) {
            return "ReceivedFineDetailedResponse(finesAmount=" + response.getFinesAmount() + ")";
        }

        if (result instanceof PlayerDTO player) {
            return "PlayerDTO(id=" + player.getId() + ", name=" + player.getName() + ", fan=" + player.isFan() + ")";
        }

        return result.getClass().getSimpleName();
    }

    private void logDashboardMatchData(String stepName, FootballMatchDetail footballMatchDetail, MatchDTO match) {
        Long footballMatchId = Optional.ofNullable(footballMatchDetail)
                .map(FootballMatchDetail::getFootballMatch)
                .map(FootballMatchDTO::getId)
                .orElse(null);

        Long matchId = match != null ? match.getId() : null;
        Integer playerCount = match != null ? match.getPlayerIdList().size() : null;

        log.info("[HOME_SETUP] {} data footballMatchId={}, matchId={}, playerCount={}",
                stepName,
                footballMatchId,
                matchId,
                playerCount);
    }
}