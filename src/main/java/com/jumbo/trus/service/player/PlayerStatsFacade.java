package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.stats.FootballAllIndividualStats;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.helper.TextWithRedirect;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.player.PlayerSetup;
import com.jumbo.trus.dto.player.stats.*;
import com.jumbo.trus.dto.player.stats.projection.IPlayerAchievementCountProjection;
import com.jumbo.trus.dto.player.stats.projection.IPlayerBeerCountProjection;
import com.jumbo.trus.dto.player.stats.projection.IPlayerGoalCountProjection;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.repository.BeerRepository;
import com.jumbo.trus.repository.GoalRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.activity.footbar.FootbarService;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.stats.FootballPlayerFact;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerStatsFacade {

    private final PlayerAchievementRepository playerAchievementRepository;
    private final BeerRepository beerRepository;
    private final GoalRepository goalRepository;
    private final ReceivedFineRepository receivedFineRepository;
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
        List<List<TextWithRedirect>> pairedPlayerStats = new ArrayList<>();
        if (playerId != null) {
            PlayerDTO playerDTO = playerService.getPlayer(playerId);
            playerSetup.setPlayer(playerDTO);
            FootballPlayerDTO footballPlayerDTO = playerDTO.getFootballPlayer();
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
            PlayerStats statsForAllSeason = setupPlayerStats(playerId, appTeam, false);

            List<TextWithRedirect> beerPairedStats = new ArrayList<>();
            boolean isPlayer = !playerDTO.isFan();
            beerPairedStats.add(new TextWithRedirect(new StringAndString("Piva v sezoně:",
                    statsForCurrentSeason.getPlayerBeerCount().getTotalBeers() + " piv / " + statsForCurrentSeason.getPlayerBeerCount().getTotalLiquors() + " panáků")));
            beerPairedStats.add(new TextWithRedirect(new StringAndString("Piva celkem:",
                    statsForAllSeason.getPlayerBeerCount().getTotalBeers() + " piv / " + statsForAllSeason.getPlayerBeerCount().getTotalLiquors() + " panáků")));
            pairedPlayerStats.add(beerPairedStats);

            if (isPlayer) {
                List<TextWithRedirect> finePairedStats = new ArrayList<>();
                finePairedStats.add(new TextWithRedirect(new StringAndString("Pokuty v sezoně:",
                        statsForCurrentSeason.getPlayerFineCount().getTotalFines() + " Kč")));
                finePairedStats.add(new TextWithRedirect(new StringAndString("Pokuty celkem:",
                        statsForAllSeason.getPlayerFineCount().getTotalFines() + " Kč")));
                pairedPlayerStats.add(finePairedStats);

                List<TextWithRedirect> goalPairedStats = new ArrayList<>();
                goalPairedStats.add(new TextWithRedirect(new StringAndString("Kanadské body v sezoně:",
                        statsForCurrentSeason.getPlayerGoalCount().getTotalGoals() + " gólů / " + statsForCurrentSeason.getPlayerGoalCount().getTotalAssists() + " asistencí")));
                goalPairedStats.add(new TextWithRedirect(new StringAndString("Kanadské body celkem:",
                        statsForAllSeason.getPlayerGoalCount().getTotalGoals() + " gólů / " + statsForAllSeason.getPlayerGoalCount().getTotalAssists() + " asistencí")));
                pairedPlayerStats.add(goalPairedStats);
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
        playerSetup.setPairedPlayerStats(pairedPlayerStats);
        playerSetup.setPlayerStats(playerStats);
        return playerSetup;
    }

    private StatisticsFilter getStatisticFilter(Long playerId, AppTeamEntity appTeam, boolean currentSeason) {
        StatisticsFilter statisticsFilter = new StatisticsFilter();
        statisticsFilter.setPlayerId(playerId);
        statisticsFilter.setAppTeam(appTeam);
        statisticsFilter.setMatchStatsOrPlayerStats(false);
        if (currentSeason) {
            statisticsFilter.setSeasonId(seasonService.getCurrentSeason(true, appTeam).getId());
        }
        else {
            statisticsFilter.setSeasonId(seasonService.getAllSeason().getId());
        }
        return statisticsFilter;
    }

    private PlayerBeerCount getPlayerBeerCount(StatisticsFilter statisticsFilter) {
        IPlayerBeerCountProjection count = isAllSeason(statisticsFilter)
                ? beerRepository.sumForPlayerAndAppTeam(statisticsFilter.getPlayerId(), statisticsFilter.getAppTeam().getId())
                : beerRepository.sumForPlayerAndAppTeamAndSeason(statisticsFilter.getPlayerId(), statisticsFilter.getAppTeam().getId(), statisticsFilter.getSeasonId());

        return count == null
                ? new PlayerBeerCount(0, 0)
                : new PlayerBeerCount(toInt(count.getTotalBeers()), toInt(count.getTotalLiquors()));
    }

    private PlayerFineCount getPlayerFineCount(StatisticsFilter statisticsFilter) {
        Long totalFines = isAllSeason(statisticsFilter)
                ? receivedFineRepository.sumFineAmountForPlayerAndAppTeam(statisticsFilter.getPlayerId(), statisticsFilter.getAppTeam().getId())
                : receivedFineRepository.sumFineAmountForPlayerAndAppTeamAndSeason(statisticsFilter.getPlayerId(), statisticsFilter.getAppTeam().getId(), statisticsFilter.getSeasonId());

        return new PlayerFineCount(toInt(totalFines));
    }

    private PlayerGoalCount getPlayerGoalCount(StatisticsFilter statisticsFilter) {
        IPlayerGoalCountProjection count = isAllSeason(statisticsFilter)
                ? goalRepository.sumForPlayerAndAppTeam(statisticsFilter.getPlayerId(), statisticsFilter.getAppTeam().getId())
                : goalRepository.sumForPlayerAndAppTeamAndSeason(statisticsFilter.getPlayerId(), statisticsFilter.getAppTeam().getId(), statisticsFilter.getSeasonId());

        return count == null
                ? new PlayerGoalCount(0, 0)
                : new PlayerGoalCount(toInt(count.getTotalGoals()), toInt(count.getTotalAssists()));
    }

    private PlayerFootbarCount getPlayerFootbarCount(StatisticsFilter statisticsFilter) {
        return new PlayerFootbarCount(footbarService.getTotalDistanceForPlayerAndSeason(
                statisticsFilter.getPlayerId(),
                statisticsFilter.getSeasonId(),
                statisticsFilter.getAppTeam().getId()
        ));
    }

    public PlayerAchievementCount getNumberOfAchievementsForPlayer(Long playerId) {
        IPlayerAchievementCountProjection count = playerAchievementRepository.countStatsByPlayerId(playerId);
        return count == null
                ? new PlayerAchievementCount(0, 0)
                : new PlayerAchievementCount(toInt(count.getTotalAchievements()), toInt(count.getAccomplishedAchievements()));
    }

    private boolean isAllSeason(StatisticsFilter statisticsFilter) {
        return statisticsFilter.getSeasonId() == com.jumbo.trus.config.Config.ALL_SEASON_ID;
    }

    private int toInt(Number number) {
        return number == null ? 0 : number.intValue();
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
