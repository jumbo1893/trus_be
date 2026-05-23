package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.IPlayerAchievementStats;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.beer.IPlayerDrinkAverageStats;
import com.jumbo.trus.dto.beer.IPlayerDrinkStats;
import com.jumbo.trus.dto.footbar.IPlayerRunningStats;
import com.jumbo.trus.dto.goal.IPlayerGoalStats;
import com.jumbo.trus.dto.helper.Redirect;
import com.jumbo.trus.dto.helper.RedirectDTO;
import com.jumbo.trus.dto.home.stats.StatsBoardData;
import com.jumbo.trus.dto.home.stats.StatsBoardRow;
import com.jumbo.trus.dto.player.PlayerDTO;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

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
        long appTeamId = appTeamEntity.getId();

        SeasonDTO currentSeason = seasonService.getCurrentSeason(true, appTeamEntity);
        SeasonDTO allSeason = seasonService.getAllSeason();

        List<StatsBoardData> statsBoardDataList = new ArrayList<>();

        statsBoardDataList.add(getPlayerAchievementData(appTeamId));
        statsBoardDataList.add(getPlayerAchievementCountData(appTeamId));

        statsBoardDataList.add(getBeerData(currentSeason, appTeamId));
        statsBoardDataList.add(getAverageBeerData(currentSeason, appTeamId));
        statsBoardDataList.add(getFineData(currentSeason, appTeamId));
        statsBoardDataList.add(getGoalData(currentSeason, appTeamId));
        statsBoardDataList.add(getFootbarData(currentSeason, appTeamId));

        statsBoardDataList.add(getBeerData(allSeason, appTeamId));
        statsBoardDataList.add(getAverageBeerData(allSeason, appTeamId));
        statsBoardDataList.add(getFineData(allSeason, appTeamId));
        statsBoardDataList.add(getGoalData(allSeason, appTeamId));
        statsBoardDataList.add(getFootbarData(allSeason, appTeamId));

        return statsBoardDataList;
    }

    private StatsBoardData getPlayerAchievementData(long appTeamId) {
        List<PlayerAchievementDTO> playerAchievementDTOList =
                playerAchievementService.getLastPlayerAchievements(
                        5,
                        getPlayerIdList(appTeamId)
                );

        if (playerAchievementDTOList.isEmpty()) {
            return null;
        }

        StatsBoardData statsBoardData = createStatsBoardData(
                "Poslední achievementy",
                "Jméno",
                "Achievement",
                "Datum"
        );

        List<StatsBoardRow> statsBoardRows = new ArrayList<>();

        for (PlayerAchievementDTO playerAchievement : playerAchievementDTOList) {
            statsBoardRows.add(
                    createPlayerRow(
                            playerAchievement.getPlayer(),
                            playerAchievement.getAchievement().getName(),
                            DateFormatter.formatDateForFrontend(
                                    playerAchievement.getAccomplishedDate()
                            )
                    )
            );
        }

        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getPlayerAchievementCountData(long appTeamId) {
        List<IPlayerAchievementStats> playerAchievementStatsList =
                playerAchievementService.getListOfPlayersOrderAccomplishedAchievements(
                        appTeamId,
                        5
                );

        if (playerAchievementStatsList.isEmpty()) {
            return null;
        }

        StatsBoardData statsBoardData = createStatsBoardData(
                "Pořadí ve splněných achievementech",
                "Jméno",
                "Splněné",
                "Nesplněné"
        );

        List<StatsBoardRow> statsBoardRows = new ArrayList<>();

        for (IPlayerAchievementStats playerAchievementStats : playerAchievementStatsList) {
            statsBoardRows.add(
                    createPlayerRow(
                            playerAchievementStats.getPlayerId(),
                            playerAchievementStats.getAccomplishedCount().toString(),
                            playerAchievementStats.getNotAccomplishedCount().toString()
                    )
            );
        }

        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getBeerData(SeasonDTO season, long appTeamId) {
        List<IPlayerDrinkStats> playerDrinkStatsList =
                beerStatsService.getListOfPlayersOrderByBeerAndLiquorNumber(
                        season.getId(),
                        appTeamId,
                        5
                );

        if (playerDrinkStatsList.isEmpty()) {
            return null;
        }

        StatsBoardData statsBoardData = createStatsBoardData(
                season.getId() == ALL_SEASON_ID
                        ? "Celkové pořadí v alkoholismu"
                        : "Pořadí v alkoholismu za sezonu " + season.getName(),
                "Jméno",
                "Piva",
                "Panáky"
        );

        List<StatsBoardRow> statsBoardRows = new ArrayList<>();

        for (IPlayerDrinkStats playerDrinkStats : playerDrinkStatsList) {
            statsBoardRows.add(
                    createPlayerRow(
                            playerDrinkStats.getPlayerId(),
                            playerDrinkStats.getBeerNumber().toString(),
                            playerDrinkStats.getLiquorNumber().toString()
                    )
            );
        }

        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getAverageBeerData(SeasonDTO season, long appTeamId) {
        List<IPlayerDrinkAverageStats> playerDrinkStatsList =
                beerStatsService.getListOfPlayersOrderByAverageBeerAndLiquorNumber(
                        season.getId(),
                        appTeamId,
                        5
                );

        if (playerDrinkStatsList.isEmpty()) {
            return null;
        }

        StatsBoardData statsBoardData = createStatsBoardData(
                season.getId() == ALL_SEASON_ID
                        ? "Celkové pořadí v pivech na zápas"
                        : "Pořadí v pivech na zápas za sezonu " + season.getName(),
                "Jméno",
                "Průměr piv",
                "Průměr panáků"
        );

        NumberRounder numberRounder = new NumberRounder();
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();

        for (IPlayerDrinkAverageStats playerDrinkAverageStats : playerDrinkStatsList) {
            statsBoardRows.add(
                    createPlayerRow(
                            playerDrinkAverageStats.getPlayerId(),
                            numberRounder.roundDoubleToString(
                                    2,
                                    playerDrinkAverageStats.getBeerNumber()
                            ),
                            numberRounder.roundDoubleToString(
                                    2,
                                    playerDrinkAverageStats.getLiquorNumber()
                            )
                    )
            );
        }

        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getFineData(SeasonDTO season, long appTeamId) {
        List<IPlayerFineStats> playerFineStatsList =
                receivedFineGetter.getListOfPlayersOrderByReceivedFineAmount(
                        season.getId(),
                        appTeamId,
                        5
                );

        if (playerFineStatsList.isEmpty()) {
            return null;
        }

        StatsBoardData statsBoardData = createStatsBoardData(
                season.getId() == ALL_SEASON_ID
                        ? "Celkové pořadí v pokutách"
                        : "Pořadí v pokutách za sezonu " + season.getName(),
                "Jméno",
                "Počet pokut",
                "Zaplaceno"
        );

        List<StatsBoardRow> statsBoardRows = new ArrayList<>();

        for (IPlayerFineStats playerFineStats : playerFineStatsList) {
            statsBoardRows.add(
                    createPlayerRow(
                            playerFineStats.getPlayerId(),
                            playerFineStats.getFineCount().toString(),
                            playerFineStats.getFineAmount() + " Kč"
                    )
            );
        }

        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getGoalData(SeasonDTO season, long appTeamId) {
        List<IPlayerGoalStats> playerGoalStatsList =
                goalService.getListOfPlayersOrderByGoalAndAssistNumber(
                        season.getId(),
                        appTeamId,
                        5
                );

        if (playerGoalStatsList.isEmpty()) {
            return null;
        }

        StatsBoardData statsBoardData = createStatsBoardData(
                season.getId() == ALL_SEASON_ID
                        ? "Celkové pořadí v kanadských bodech"
                        : "Pořadí v kanadských bodech za sezonu " + season.getName(),
                "Jméno",
                "Počet gólů",
                "Počet asistencí"
        );

        List<StatsBoardRow> statsBoardRows = new ArrayList<>();

        for (IPlayerGoalStats playerGoalStats : playerGoalStatsList) {
            statsBoardRows.add(
                    createPlayerRow(
                            playerGoalStats.getPlayerId(),
                            playerGoalStats.getGoalNumber().toString(),
                            playerGoalStats.getAssistNumber().toString()
                    )
            );
        }

        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData getFootbarData(SeasonDTO season, long appTeamId) {
        List<IPlayerRunningStats> playerRunningStatsList =
                footbarSessionGetter.getListOfPlayersOrderByAverageTotalDistance(
                        season.getId(),
                        appTeamId,
                        5
                );

        if (playerRunningStatsList.isEmpty()) {
            return null;
        }

        StatsBoardData statsBoardData = createStatsBoardData(
                season.getId() == ALL_SEASON_ID
                        ? "Celkové pořadí v naběhaných km za zápas"
                        : "Pořadí v naběhaných km za sezonu " + season.getName(),
                "Jméno",
                "Průměr na zápas",
                "Celkem"
        );

        NumberRounder numberRounder = new NumberRounder();
        List<StatsBoardRow> statsBoardRows = new ArrayList<>();

        for (IPlayerRunningStats playerRunningStats : playerRunningStatsList) {
            statsBoardRows.add(
                    createPlayerRow(
                            playerRunningStats.getPlayerId(),
                            numberRounder.roundDoubleToString(
                                    2,
                                    playerRunningStats.getAverageDistance()
                            ),
                            numberRounder.roundDoubleToString(
                                    2,
                                    playerRunningStats.getTotalDistance()
                            )
                    )
            );
        }

        statsBoardData.setRows(statsBoardRows);
        return statsBoardData;
    }

    private StatsBoardData createStatsBoardData(
            String title,
            String firstHeader,
            String secondHeader,
            String thirdHeader
    ) {
        StatsBoardData statsBoardData = new StatsBoardData();
        statsBoardData.setTitle(title);
        statsBoardData.setHeaders(
                Arrays.asList(firstHeader, secondHeader, thirdHeader)
        );
        return statsBoardData;
    }

    private StatsBoardRow createPlayerRow(Long playerId, String... values) {
        PlayerDTO player = playerService.getPlayer(playerId);
        return createPlayerRow(player, values);
    }

    private StatsBoardRow createPlayerRow(PlayerDTO player, String... values) {
        List<String> columns = new ArrayList<>();
        columns.add(player.getName());
        columns.addAll(Arrays.asList(values));

        StatsBoardRow statsBoardRow = new StatsBoardRow();
        statsBoardRow.setColumns(columns);
        statsBoardRow.setRedirect(getPlayerRedirect(player));

        return statsBoardRow;
    }

    private List<Long> getPlayerIdList(long appTeamId) {
        return playerService.convertPlayerListToPlayerIdList(
                playerService.getAll(appTeamId)
        );
    }

    private RedirectDTO getPlayerRedirect(PlayerDTO playerDTO) {
        RedirectDTO redirectDTO = new RedirectDTO();
        redirectDTO.setRedirect(Redirect.VIEW_PLAYER);
        redirectDTO.setPlayer(playerDTO);
        return redirectDTO;
    }
}