package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.stats.FootballAllIndividualStats;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.helper.TextWithRedirect;
import com.jumbo.trus.dto.player.PlayerSetup;
import com.jumbo.trus.dto.player.stats.*;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.activity.footbar.FootbarService;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.stats.FootballPlayerFact;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.helper.DetailedResponseHelper;
import com.jumbo.trus.service.receivedFine.ReceivedFineGetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerStatsFacade {

    private final PlayerAchievementMapper playerAchievementMapper;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final ReceivedFineGetter receivedFineService;
    private final DetailedResponseHelper detailedResponseHelper;
    private final SeasonService seasonService;
    private final FootballPlayerService footballPlayerService;
    private final FootballPlayerStatsService footballPlayerStatsService;
    private final PlayerService playerService;
    private final FootballPlayerFact footballPlayerFact;
    private final FootbarService footbarService;

    public PlayerStats setupPlayerStats(Long playerId, AppTeamEntity appTeam, boolean currentSeason) {
        PlayerStats playerStats = new PlayerStats();
        StatisticsFilter statisticsFilter = getStatisticFilter(playerId, appTeam, currentSeason);
        playerStats.setPlayerAchievementCount(getNumberOfAchievementsForPlayer(playerId));
        playerStats.setPlayerBeerCount(getPlayerBeerCount(statisticsFilter));
        playerStats.setPlayerFineCount(getPlayerFineCount(statisticsFilter));
        playerStats.setPlayerGoalCount(getPlayerGoalCount(statisticsFilter));
        playerStats.setPlayerFootbarCount(getPlayerFootbarCount(statisticsFilter));
        if (currentSeason) {
            playerStats.setSeason(seasonService.getCurrentSeason(true, appTeam));
        } else {
            playerStats.setSeason(seasonService.getAllSeason());
        }
        return playerStats;
    }

    public PlayerSetup setupPlayer(Long playerId, AppTeamEntity appTeam) {
        PlayerSetup playerSetup = new PlayerSetup();
        List<FootballPlayerDTO> playerList = new ArrayList<>(footballPlayerService.getAllPastPlayersByCurrentTeam(appTeam));
        playerList.add(0, footballPlayerService.noPlayer());
        playerSetup.setFootballPlayerList(playerList);
        List<TextWithRedirect> playerStats = new ArrayList<>();
        if (playerId != null) {
            playerSetup.setPlayer(playerService.getPlayer(playerId));
            FootballPlayerDTO footballPlayerDTO = playerSetup.getPlayer().getFootballPlayer();
            if (footballPlayerDTO != null) {
                playerSetup.setPrimaryFootballPlayer(footballPlayerDTO);
                FootballAllIndividualStats stats = footballPlayerStatsService.getPlayerStatsForPlayer(footballPlayerDTO.getId(), appTeam);
                playerStats.add(new TextWithRedirect(returnStringForGoalsAndMatchesFromLeague(stats)));
                playerStats.add(new TextWithRedirect(returnStringForCardsFromLeague(stats)));
                playerStats.add(new TextWithRedirect(returnStringForStarOfMatchLeague(stats)));
            } else {
                playerSetup.setPrimaryFootballPlayer(footballPlayerService.noPlayer());
            }
            PlayerStats statsForCurrentSeason = setupPlayerStats(playerId, appTeam, true);
            boolean isPlayer = !playerService.getPlayer(playerId).isFan();
            playerStats.add(new TextWithRedirect(new StringAndString("Piva v sezoně:",
                    statsForCurrentSeason.getPlayerBeerCount().getTotalBeers() + " piv / " + statsForCurrentSeason.getPlayerBeerCount().getTotalLiquors() + " panáků")));
            if (isPlayer) {
                playerStats.add(new TextWithRedirect(new StringAndString("Pokuty v sezoně:",
                        statsForCurrentSeason.getPlayerFineCount().getTotalFines() + " Kč")));
                playerStats.add(new TextWithRedirect(new StringAndString("Kanadské body v sezoně:",
                        statsForCurrentSeason.getPlayerGoalCount().getTotalGoals() + " gólů / " + statsForCurrentSeason.getPlayerGoalCount().getTotalAssists() + " asistencí")));
            }
            PlayerStats statsForAllSeason = setupPlayerStats(playerId, appTeam, false);
            playerStats.add(new TextWithRedirect(new StringAndString("Piva celkem:",
                    statsForAllSeason.getPlayerBeerCount().getTotalBeers() + " piv / " + statsForAllSeason.getPlayerBeerCount().getTotalLiquors() + " panáků")));
            if (isPlayer) {
                playerStats.add(new TextWithRedirect(new StringAndString("Pokuty celkem:",
                        statsForAllSeason.getPlayerFineCount().getTotalFines() + " Kč")));
                playerStats.add(new TextWithRedirect(new StringAndString("Kanadské body celkem:",
                        statsForAllSeason.getPlayerGoalCount().getTotalGoals() + " gólů / " + statsForAllSeason.getPlayerGoalCount().getTotalAssists() + " asistencí")));
            }
            if (footballPlayerDTO != null) {
                for (StringAndString fact : footballPlayerFact.getFactsForPlayer(footballPlayerDTO.getId(), appTeam)) {
                    if (fact != null) {
                        playerStats.add(new TextWithRedirect(fact));
                    }
                }
            }
        } else {
            playerSetup.setPrimaryFootballPlayer(footballPlayerService.noPlayer());
        }
        playerSetup.setPlayerStats(playerStats);
        return playerSetup;
    }

    private StatisticsFilter getStatisticFilter(Long playerId, AppTeamEntity appTeam, boolean currentSeason) {
        StatisticsFilter statisticsFilter = new StatisticsFilter();
        statisticsFilter.setPlayerId(playerId);
        statisticsFilter.setMatchStatsOrPlayerStats(false);
        if (currentSeason) {
            statisticsFilter.setSeasonId(seasonService.getCurrentSeason(true, appTeam).getId());
        }
        return statisticsFilter;
    }

    private PlayerBeerCount getPlayerBeerCount(StatisticsFilter statisticsFilter) {
        BeerDetailedResponse beerDetailedResponse = new BeerDetailedResponse(detailedResponseHelper.getAllDetailed(statisticsFilter, DetailedResponseHelper.DetailedType.BEER));
        return new PlayerBeerCount(beerDetailedResponse.getTotalBeers(), beerDetailedResponse.getTotalLiquors());
    }

    private PlayerFineCount getPlayerFineCount(StatisticsFilter statisticsFilter) {
        ReceivedFineDetailedResponse receivedFineDetailedResponse = receivedFineService.getAllDetailed(statisticsFilter);
        return new PlayerFineCount(receivedFineDetailedResponse.getFinesAmount());
    }

    private PlayerGoalCount getPlayerGoalCount(StatisticsFilter statisticsFilter) {
        GoalDetailedResponse goalDetailedResponse = new GoalDetailedResponse(detailedResponseHelper.getAllDetailed(statisticsFilter, DetailedResponseHelper.DetailedType.GOAL));
        return new PlayerGoalCount(goalDetailedResponse.getTotalGoals(), goalDetailedResponse.getTotalAssists());
    }

    private PlayerFootbarCount getPlayerFootbarCount(StatisticsFilter statisticsFilter) {
        return new PlayerFootbarCount(footbarService.getTotalDistanceForPlayerAndSeason(statisticsFilter.getPlayerId(), statisticsFilter.getSeasonId()));
    }

    public PlayerAchievementCount getNumberOfAchievementsForPlayer(Long playerId) {
        List<PlayerAchievementDTO> list = playerAchievementRepository
                .findAllByPlayerId(playerId)
                .stream()
                .map(playerAchievementMapper::toDTO)
                .toList();

        int total = list.size();
        int accomplished = (int) list.stream()
                .filter(PlayerAchievementDTO::getAccomplished)
                .count();

        return new PlayerAchievementCount(total, accomplished);
    }

    private StringAndString returnStringForGoalsAndMatchesFromLeague(FootballAllIndividualStats stats) {
        return new StringAndString("Zápasy v PKFL:", stats.getMatches() + " zápasů / " + stats.getGoals() + " gólů");
    }

    private StringAndString returnStringForCardsFromLeague(FootballAllIndividualStats stats) {
        return new StringAndString("Karty v PKFL:", stats.getYellowCards() + " žlutých / " + stats.getRedCards() + " červených");
    }

    private StringAndString returnStringForStarOfMatchLeague(FootballAllIndividualStats stats) {
        return new StringAndString("Počet hvězd zápasu:", stats.getBestPlayer() + " hvězd");
    }
}
