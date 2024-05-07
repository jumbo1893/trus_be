package com.jumbo.trus.service.beer;

import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.dto.stats.PlayerStatsDTO;
import com.jumbo.trus.dto.stats.StatsDTO;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.mapper.BeerDetailedMapper;
import com.jumbo.trus.mapper.BeerMapper;
import com.jumbo.trus.service.PlayerService;
import com.jumbo.trus.service.beer.helper.AverageBeer;
import com.jumbo.trus.service.helper.NumberRounder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Service
public class BeerStatsService {

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private PlayerService playerService;


    @Autowired
    private BeerDetailedMapper beerDetailedMapper;

    @Autowired
    private BeerMapper beerMapper;

    public List<StatsDTO> getBeerStatistics(StatisticsFilter filter) {
        Long seasonId = filter.getSeasonId();
        List<StatsDTO> statsDTOList = new ArrayList<>();
        statsDTOList.add(getMaxNumber(true, seasonId));
        statsDTOList.add(getMaxNumber(false, seasonId));
        statsDTOList.add(getAverageNumber(true, seasonId));
        statsDTOList.add(getAverageNumber(false, seasonId));
        statsDTOList.add(getAverageNumberByGoal(true, seasonId));
        statsDTOList.add(getAverageNumberByGoal(false, seasonId));
        statsDTOList.add(getAverageNumberByAssist(true, seasonId));
        statsDTOList.add(getAverageNumberByAssist(false, seasonId));
        return statsDTOList;
    }


    private StatsDTO getMaxNumber(boolean forBeer, Long seasonId) {
        StatsDTO statsDTO = new StatsDTO();
        statsDTO.setDropdownText(forBeer ? "Max počet piv" : "Max počet panáků");
        List<PlayerStatsDTO> playerStatsList = new ArrayList<>();
        List<BeerDetailedDTO> maxBeerList = findDetailedPlayersWithMaximumBeers(forBeer, seasonId);
        for (BeerDetailedDTO playerBeer : maxBeerList) {
            int beverageNumber = getBeerOrLiquorFromBeer(playerBeer, forBeer);
            PlayerStatsDTO playerStatsDTO = new PlayerStatsDTO(playerBeer.getPlayer(), beverageNumber + (forBeer ?" piv v zápase " : " panáků v zápase ") + getMatchText(playerBeer));
            playerStatsList.add(playerStatsDTO);
        }
        statsDTO.setPlayerStats(playerStatsList);
        return statsDTO;
    }

    public List<BeerDetailedDTO> findDetailedPlayersWithMaximumBeers(boolean forBeer, Long seasonId) {
        List<BeerEntity> beerEntityList = isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getMaxBeer() : beerRepository.getMaxLiquor()) : (forBeer ? beerRepository.getMaxBeer(seasonId) : beerRepository.getMaxLiquor(seasonId));
        return beerEntityList.stream().map(beerDetailedMapper::toDTO).toList();
    }

    public List<AverageBeer> getAverageNumberList(boolean forBeer, Long seasonId, String orderBy) {
        Sort sort = Sort.by(Sort.Direction.DESC, orderBy);
        return isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getAverageBeer(sort) : beerRepository.getAverageLiquor(sort))
                : forBeer ? beerRepository.getAverageBeer(seasonId, sort) : beerRepository.getAverageLiquor(seasonId, sort);
    }
    private StatsDTO getAverageNumber(boolean forBeer, Long seasonId) {
        List<AverageBeer> averageBeerList = getAverageNumberList(forBeer, seasonId, "avgBeerPerMatch");
        return averageNumberHelper(averageBeerList, forBeer ? "Průměrný počet piv" : "Průměrný počet panáků", forBeer ?" piv na zápas " : " panáků na zápas ");
    }

    private StatsDTO getAverageNumberByGoal(boolean forBeer, Long seasonId) {
        List<AverageBeer> averageBeerList = isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getGoalBeerRatio() : beerRepository.getGoalLiquorRatio()) : forBeer ? beerRepository.getGoalBeerRatio(seasonId) : beerRepository.getGoalLiquorRatio(seasonId);
        return averageNumberHelper(averageBeerList, forBeer ? "Počet piv na gól" : "Počet panáků na gól", forBeer ?" piv na 1 gól " : " panáků na 1 gól ");
    }

    private StatsDTO getAverageNumberByAssist(boolean forBeer, Long seasonId) {
        List<AverageBeer> averageBeerList = isForAllSeasons(seasonId) ? (forBeer ? beerRepository.getAssistBeerRatio() : beerRepository.getAssistLiquorRatio()) : forBeer ? beerRepository.getAssistBeerRatio(seasonId) : beerRepository.getAssistLiquorRatio(seasonId);
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
