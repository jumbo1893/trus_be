package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.IPlayerAchievementStats;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.beer.IPlayerDrinkAverageStats;
import com.jumbo.trus.dto.beer.IPlayerDrinkStats;
import com.jumbo.trus.dto.footbar.IPlayerRunningStats;
import com.jumbo.trus.dto.goal.IPlayerGoalStats;
import com.jumbo.trus.dto.home.stats.StatsBoardData;
import com.jumbo.trus.dto.home.stats.StatsBoardRow;
import com.jumbo.trus.dto.receivedfine.IPlayerFineStats;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.GoalService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.activity.footbar.session.FootbarSessionGetter;
import com.jumbo.trus.service.beer.BeerStatsService;
import com.jumbo.trus.service.helper.DateFormatter;
import com.jumbo.trus.service.helper.NumberRounder;
import com.jumbo.trus.service.player.PlayerAchievementService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.receivedFine.ReceivedFineGetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsBoardDataService {

    private final PlayerAchievementService playerAchievementService;
    private final PlayerService playerService;
    private final BeerStatsService beerStatsService;
    private final SeasonService seasonService;
    private final ReceivedFineGetter receivedFineGetter;
    private final GoalService goalService;
    private final FootbarSessionGetter footbarSessionGetter;


    public List<StatsBoardData> getStatsBoardDataList(AppTeamEntity appTeamEntity) {
        List<StatsBoardData> statsBoardDataList = new ArrayList<>();
        SeasonDTO currentSeason = seasonService.getCurrentSeason(true, appTeamEntity);
        SeasonDTO allSeason = seasonService.getAllSeason();
        statsBoardDataList.add(getPlayerAchievementData(appTeamEntity.getId()));
        statsBoardDataList.add(getPlayerAchievementCountData(appTeamEntity.getId()));
        statsBoardDataList.add(getBeerData(currentSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getAverageBeerData(currentSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getFineData(currentSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getGoalData(currentSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getFootbarData(currentSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getBeerData(allSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getAverageBeerData(allSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getFineData(allSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getGoalData(allSeason, appTeamEntity.getId()));
        statsBoardDataList.add(getFootbarData(allSeason, appTeamEntity.getId()));
        return statsBoardDataList;
    }

    private StatsBoardData getPlayerAchievementData(long appTeamId) {
        List<PlayerAchievementDTO> playerAchievementDTOList = playerAchievementService.getLastPlayerAchievements(5, getPlayerIdList(appTeamId));
        if (playerAchievementDTOList.isEmpty()) {
            return null;
        }
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle("Poslední achievementy");
        statsBoardData.setHeaders(Arrays.asList("Jméno", "Achievement", "Datum"));
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();
        for (PlayerAchievementDTO playerAchievement : playerAchievementDTOList) {
            StatsBoardRow statsBoardRow = new StatsBoardRow();
            statsBoardRow.setColumns(Arrays.asList(playerAchievement.getPlayer().getName(), playerAchievement.getAchievement().getName(),  DateFormatter.formatDateForFrontend(playerAchievement.getAccomplishedDate())));
            statsBoardRows.add(statsBoardRow);
        }
        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getPlayerAchievementCountData(long appTeamId) {
        List<IPlayerAchievementStats> playerAchievementStatsList = playerAchievementService.getListOfPlayersOrderAccomplishedAchievements(appTeamId, 5);
        if (playerAchievementStatsList.isEmpty()) {
            return null;
        }
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle("Pořadí ve splněných achievementech");
        statsBoardData.setHeaders(Arrays.asList("Jméno", "Splněné", "Nesplněné"));
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();
        for (IPlayerAchievementStats playerAchievementStats : playerAchievementStatsList) {
            StatsBoardRow statsBoardRow = new StatsBoardRow();
            statsBoardRow.setColumns(Arrays.asList(playerAchievementStats.getPlayerName(), playerAchievementStats.getAccomplishedCount().toString(), playerAchievementStats.getNotAccomplishedCount().toString()));
            statsBoardRows.add(statsBoardRow);
        }
        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getBeerData(SeasonDTO season, long appTeamId) {
        List<IPlayerDrinkStats> playerDrinkStatsList = beerStatsService.getListOfPlayersOrderByBeerAndLiquorNumber(season.getId(), appTeamId, 5);
        if (playerDrinkStatsList.isEmpty()) {
            return null;
        }
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle(season.getId() == ALL_SEASON_ID ? "Celkové pořadí v alkoholismu" : "Pořadí v alkoholismu za sezonu " + season.getName());
        statsBoardData.setHeaders(Arrays.asList("Jméno", "Piva", "Panáky"));
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();
        for (IPlayerDrinkStats playerDrinkStats : playerDrinkStatsList) {
            StatsBoardRow statsBoardRow = new StatsBoardRow();
            statsBoardRow.setColumns(Arrays.asList(playerDrinkStats.getPlayerName(), playerDrinkStats.getBeerNumber().toString(), playerDrinkStats.getLiquorNumber().toString()));
            statsBoardRows.add(statsBoardRow);
        }
        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getAverageBeerData(SeasonDTO season, long appTeamId) {
        List<IPlayerDrinkAverageStats> playerDrinkStatsList = beerStatsService.getListOfPlayersOrderByAverageBeerAndLiquorNumber(season.getId(), appTeamId, 5);
        if (playerDrinkStatsList.isEmpty()) {
            return null;
        }
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle(season.getId() == ALL_SEASON_ID ? "Celkové pořadí v pivech na zápas" : "Pořadí v pivech na zápas za sezonu " + season.getName());
        statsBoardData.setHeaders(Arrays.asList("Jméno", "Průměr piv", "Průměr panáků"));
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();
        NumberRounder numberRounder = new NumberRounder();
        for (IPlayerDrinkAverageStats playerDrinkAverageStats : playerDrinkStatsList) {
            StatsBoardRow statsBoardRow = new StatsBoardRow();
            statsBoardRow.setColumns(Arrays.asList(playerDrinkAverageStats.getPlayerName(),
                    numberRounder.roundDoubleToString(2, playerDrinkAverageStats.getBeerNumber()),
                    numberRounder.roundDoubleToString(2, playerDrinkAverageStats.getLiquorNumber())));
            statsBoardRows.add(statsBoardRow);
        }
        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getFineData(SeasonDTO season, long appTeamId) {
        List<IPlayerFineStats> playerFineStatsList = receivedFineGetter.getListOfPlayersOrderByReceivedFineAmount(season.getId(), appTeamId, 5);
        if (playerFineStatsList.isEmpty()) {
            return null;
        }
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle(season.getId() == ALL_SEASON_ID ? "Celkové pořadí v pokutách" : "Pořadí v pokutách za sezonu " + season.getName());
        statsBoardData.setHeaders(Arrays.asList("Jméno", "Počet pokut", "Zaplaceno"));
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();
        for (IPlayerFineStats playerFineStats : playerFineStatsList) {
            StatsBoardRow statsBoardRow = new StatsBoardRow();
            statsBoardRow.setColumns(Arrays.asList(playerFineStats.getPlayerName(), playerFineStats.getFineCount().toString(), playerFineStats.getFineAmount().toString() + " Kč"));
            statsBoardRows.add(statsBoardRow);
        }
        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getGoalData(SeasonDTO season, long appTeamId) {
        List<IPlayerGoalStats> playerGoalStatsList = goalService.getListOfPlayersOrderByGoalAndAssistNumber(season.getId(), appTeamId, 5);
        if (playerGoalStatsList.isEmpty()) {
            return null;
        }
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle(season.getId() == ALL_SEASON_ID ? "Celkové pořadí v kanadských bodech" : "Pořadí v kanadských bodech za sezonu " + season.getName());
        statsBoardData.setHeaders(Arrays.asList("Jméno", "Počet gólů", "Počet asistencí"));
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();
        for (IPlayerGoalStats playerGoalStats : playerGoalStatsList) {
            StatsBoardRow statsBoardRow = new StatsBoardRow();
            statsBoardRow.setColumns(Arrays.asList(playerGoalStats.getPlayerName(), playerGoalStats.getGoalNumber().toString(), playerGoalStats.getAssistNumber().toString()));
            statsBoardRows.add(statsBoardRow);
        }
        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getFootbarData(SeasonDTO season, long appTeamId) {
        List<IPlayerRunningStats> playerRunningStatsList = footbarSessionGetter.getListOfPlayersOrderByAverageTotalDistance(season.getId(), appTeamId, 5);
        if (playerRunningStatsList.isEmpty()) {
            return null;
        }
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle(season.getId() == ALL_SEASON_ID ? "Celkové pořadí v naběhaných km za zápas" : "Pořadí v naběhaných km za sezonu " + season.getName());
        statsBoardData.setHeaders(Arrays.asList("Jméno", "Průměr na zápas", "Celkem"));
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();
        NumberRounder numberRounder = new NumberRounder();
        for (IPlayerRunningStats playerRunningStats : playerRunningStatsList) {
            StatsBoardRow statsBoardRow = new StatsBoardRow();
            statsBoardRow.setColumns(Arrays.asList(playerRunningStats.getPlayerName(),
                    numberRounder.roundDoubleToString(2, playerRunningStats.getAverageDistance()),
                    playerRunningStats.getTotalDistance().toString()));
            statsBoardRows.add(statsBoardRow);
        }
        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private List<Long> getPlayerIdList(long appTeamId) {
        return playerService.convertPlayerListToPlayerIdList(playerService.getAll(appTeamId));
    }

}
