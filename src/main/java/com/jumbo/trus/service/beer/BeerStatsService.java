package com.jumbo.trus.service.beer;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.dto.stats.PlayerStatsDTO;
import com.jumbo.trus.dto.stats.StatsDTO;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.mapper.BeerDetailedMapper;
import com.jumbo.trus.service.beer.helper.AverageBeer;
import com.jumbo.trus.service.helper.NumberRounder;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Service
@RequiredArgsConstructor
public class BeerStatsService {

    private final BeerRepository beerRepository;
    private final PlayerService playerService;
    private final BeerDetailedMapper beerDetailedMapper;

    public List<StatsDTO> getBeerStatistics(StatisticsFilter filter) {
        Long seasonId = filter.getSeasonId();
        List<StatsDTO> statsDTOList = new ArrayList<>();
        Long appTeamId = filter.getAppTeam().getId();
        statsDTOList.add(getMaxNumber(true, seasonId, appTeamId));
        statsDTOList.add(getMaxNumber(false, seasonId, appTeamId));
        statsDTOList.add(getAverageNumber(true, seasonId, appTeamId));
        statsDTOList.add(getAverageNumber(false, seasonId, appTeamId));
        statsDTOList.add(getAverageNumberByGoal(true, seasonId, appTeamId));
        statsDTOList.add(getAverageNumberByGoal(false, seasonId, appTeamId));
        statsDTOList.add(getAverageNumberByAssist(true, seasonId, appTeamId));
        statsDTOList.add(getAverageNumberByAssist(false, seasonId, appTeamId));
        return statsDTOList;
    }


    private StatsDTO getMaxNumber(boolean forBeer, Long seasonId, long appTeamId) {
        StatsDTO statsDTO = new StatsDTO();
        statsDTO.setDropdownText(forBeer ? "Max počet piv" : "Max počet panáků");
        List<PlayerStatsDTO> playerStatsList = new ArrayList<>();
        List<BeerDetailedDTO> maxBeerList = findDetailedPlayersWithMaximumBeers(forBeer, seasonId, appTeamId);
        for (BeerDetailedDTO playerBeer : maxBeerList) {
            int beverageNumber = getBeerOrLiquorFromBeer(playerBeer, forBeer);
            PlayerStatsDTO playerStatsDTO = new PlayerStatsDTO(playerBeer.getPlayer(), beverageNumber + (forBeer ?" piv v zápase " : " panáků v zápase ") + getMatchText(playerBeer));
            playerStatsList.add(playerStatsDTO);
        }
        statsDTO.setPlayerStats(playerStatsList);
        return statsDTO;
    }

    private List<BeerDetailedDTO> findDetailedPlayersWithMaximumBeers(boolean forBeer, Long seasonId, long appTeamId) {
        List<BeerEntity> beerEntityList = isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getMaxBeer(appTeamId) : beerRepository.getMaxLiquor(appTeamId)) : (forBeer ? beerRepository.getMaxBeer(seasonId, appTeamId) : beerRepository.getMaxLiquor(seasonId, appTeamId));
        return beerEntityList.stream().map(beerDetailedMapper::toDTO).toList();
    }

    public List<AverageBeer> getAverageNumberList(boolean forBeer, Long seasonId, String orderBy, long appTeamId) {
        Sort sort = Sort.by(Sort.Direction.DESC, orderBy);
        return isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getAverageBeer(sort, appTeamId) : beerRepository.getAverageLiquor(sort, appTeamId))
                : forBeer ? beerRepository.getAverageBeer(seasonId, appTeamId, sort) : beerRepository.getAverageLiquor(seasonId, appTeamId, sort);
    }

    public List<AverageBeer> getAverageBeerAndLiquorListOrderByTotalBeer(Long seasonId, long appTeamId) {
        return isForAllSeasons(seasonId) ? beerRepository.getAverageBeerAndLiquorSum(appTeamId) : beerRepository.getAverageBeerAndLiquorSum(seasonId, appTeamId);
    }

    private StatsDTO getAverageNumber(boolean forBeer, Long seasonId, long appTeamId) {
        List<AverageBeer> averageBeerList = getAverageNumberList(forBeer, seasonId, "avgBeerPerMatch", appTeamId);
        return averageNumberHelper(averageBeerList, forBeer ? "Průměrný počet piv" : "Průměrný počet panáků", forBeer ?" piv na zápas " : " panáků na zápas ");
    }

    private StatsDTO getAverageNumberByGoal(boolean forBeer, Long seasonId, long appTeamId) {
        List<AverageBeer> averageBeerList = isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getGoalBeerRatio(appTeamId) : beerRepository.getGoalLiquorRatio(appTeamId)) : forBeer ? beerRepository.getGoalBeerRatio(seasonId, appTeamId) : beerRepository.getGoalLiquorRatio(seasonId, appTeamId);
        return averageNumberHelper(averageBeerList, forBeer ? "Počet piv na gól" : "Počet panáků na gól", forBeer ?" piv na 1 gól " : " panáků na 1 gól ");
    }

    private StatsDTO getAverageNumberByAssist(boolean forBeer, Long seasonId, long appTeamId) {
        List<AverageBeer> averageBeerList = isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getAssistBeerRatio(appTeamId) : beerRepository.getAssistLiquorRatio(appTeamId)) : forBeer ? beerRepository.getAssistBeerRatio(seasonId, appTeamId) : beerRepository.getAssistLiquorRatio(seasonId, appTeamId);
        return averageNumberHelper(averageBeerList, forBeer ? "Počet piv na asistenci" : "Počet panáků na asistenci", forBeer ?" piv na 1 asistenci " : " panáků na 1 asistenci ");
    }

    private StatsDTO averageNumberHelper(List<AverageBeer> averageBeerList, String dropDownText, String subtitleText) {
        StatsDTO statsDTO = new StatsDTO();
        statsDTO.setDropdownText(dropDownText);
        List<PlayerStatsDTO> playerStatsList = new ArrayList<>();
        for (AverageBeer averageBeer : averageBeerList) {
            NumberRounder numberRounder = new NumberRounder();
            PlayerStatsDTO playerStatsDTO = new PlayerStatsDTO(playerService.getPlayer(averageBeer.getPlayerId()), numberRounder.roundFloatToString(2, averageBeer.getAvgBeerPerMatch()) + (subtitleText));
            playerStatsList.add(playerStatsDTO);
        }
        statsDTO.setPlayerStats(playerStatsList);
        return statsDTO;
    }

    private String getMatchText(BeerDetailedDTO beerDetailedDTO) {
        MatchHelper matchHelper = new MatchHelper(beerDetailedDTO.getMatch());
        return matchHelper.getMatchWithOpponentNameAndDate();
    }

    private int getBeerOrLiquorFromBeer(BeerDetailedDTO beerDetailedDTO, boolean beer) {
        if (beer) {
            return beerDetailedDTO.getBeerNumber();
        }
        return beerDetailedDTO.getLiquorNumber();
    }

    private boolean isForAllSeasons(Long seasonId) {
        return seasonId == null || seasonId == ALL_SEASON_ID;
    }


}
