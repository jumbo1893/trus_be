package com.jumbo.trus.service.beer;

import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.dto.stats.PlayerStatsDTO;
import com.jumbo.trus.dto.stats.StatsDTO;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.entity.repository.MatchRepository;
import com.jumbo.trus.entity.repository.PlayerRepository;
import com.jumbo.trus.entity.repository.specification.BeerStatsSpecification;
import com.jumbo.trus.mapper.BeerDetailedMapper;
import com.jumbo.trus.mapper.BeerMapper;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.NotificationService;
import com.jumbo.trus.service.PlayerService;
import com.jumbo.trus.service.beer.helper.AverageBeer;
import com.jumbo.trus.service.helper.NumberRounder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Service
public class BeerStatsService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private MatchService matchService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private BeerMapper beerMapper;

    @Autowired
    private BeerDetailedMapper beerDetailedMapper;

    @Autowired
    private NotificationService notificationService;

    private List<BeerDetailedDTO> beerList = new ArrayList<>();

    private Long seasonId;

    private void setDetailedBeerList(StatisticsFilter filter) {
        BeerStatsSpecification beerSpecification = new BeerStatsSpecification(filter);
        beerList = beerRepository.findAll(beerSpecification, PageRequest.of(0, filter.getLimit())).stream().map(beerDetailedMapper::toDTO).toList();
    }

    private boolean isPlayerInBeerDetail(BeerDetailedDTO beer, PlayerDTO player) {
        return beer.getPlayer().equals(player);
    }

    public List<StatsDTO> getBeerStatistics(StatisticsFilter filter) {
        setDetailedBeerList(filter);
        seasonId = filter.getSeasonId();
        List<StatsDTO> statsDTOList = new ArrayList<>();
        statsDTOList.add(getMaxNumber(true));
        statsDTOList.add(getMaxNumber(false));
        statsDTOList.add(getAverageNumber(true));
        statsDTOList.add(getAverageNumber(false));
        statsDTOList.add(getAverageNumberByGoal(true));
        statsDTOList.add(getAverageNumberByGoal(false));
        return statsDTOList;
    }


    private StatsDTO getMaxNumber(boolean forBeer) {
        StatsDTO statsDTO = new StatsDTO();
        statsDTO.setDropdownText(forBeer ? "Max počet piv" : "Max počet panáků");
        List<PlayerStatsDTO> playerStatsList = new ArrayList<>();
        List<BeerEntity> beerEntityList = isForAllSeasons() ? (forBeer ? beerRepository.getMaxBeer() : beerRepository.getMaxLiquor()) : (forBeer ? beerRepository.getMaxBeer(seasonId) : beerRepository.getMaxLiquor(seasonId));
        List<BeerDetailedDTO> maxBeerList = beerEntityList.stream().map(beerDetailedMapper::toDTO).toList();
        for (BeerDetailedDTO playerBeer : maxBeerList) {
            int beverageNumber = getBeerOrLiquorFromBeer(playerBeer, forBeer);
            PlayerStatsDTO playerStatsDTO = new PlayerStatsDTO(playerBeer.getPlayer(), beverageNumber + (forBeer ?" piv v zápase " : " panáků v zápase ") + getMatchText(playerBeer));
            playerStatsList.add(playerStatsDTO);
        }
        statsDTO.setPlayerStats(playerStatsList);
        return statsDTO;
    }

    private StatsDTO getAverageNumber(boolean forBeer) {
        List<AverageBeer> averageBeerList = isForAllSeasons() ? (forBeer ? beerRepository.getAverageBeer() : beerRepository.getAverageLiquor()) : forBeer ? beerRepository.getAverageBeer(seasonId) : beerRepository.getAverageLiquor(seasonId);
        return averageNumberHelper(averageBeerList, forBeer ? "Průměrný počet piv" : "Průměrný počet panáků", forBeer ?" piv na zápas " : " panáků na zápas ");
    }

    private StatsDTO getAverageNumberByGoal(boolean forBeer) {
        List<AverageBeer> averageBeerList = isForAllSeasons() ? (forBeer ? beerRepository.getGoalBeerRatio() : beerRepository.getGoalLiquorRatio()) : forBeer ? beerRepository.getGoalBeerRatio(seasonId) : beerRepository.getGoalLiquorRatio(seasonId);
        return averageNumberHelper(averageBeerList, forBeer ? "Počet piv na gól" : "Počet panáků na gól", forBeer ?" piv na 1 gól " : " panáků na 1 gól ");
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

    private boolean isForAllSeasons() {
        return seasonId == null || seasonId == ALL_SEASON_ID;
    }


}
