package com.jumbo.trus.service.helper;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.entity.repository.GoalRepository;
import com.jumbo.trus.entity.repository.specification.BeerStatsSpecification;
import com.jumbo.trus.entity.repository.specification.GoalStatsSpecification;
import com.jumbo.trus.mapper.BeerDetailedMapper;
import com.jumbo.trus.mapper.GoalDetailedMapper;
import com.jumbo.trus.service.order.OrderDetailedDTOByFirstNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DetailedResponseHelper {

    private final GoalRepository goalRepository;
    private final GoalDetailedMapper goalDetailedMapper;
    private final BeerRepository beerRepository;
    private final BeerDetailedMapper beerDetailedMapper;

    public DetailedResponse getAllDetailed(StatisticsFilter filter, DetailedType detailedType) {
        // Získání gólů z databáze
        List<DetailedDTO> detailList = getFilteredDetails(filter, detailedType);

        // Inicializace dat pro odpověď
        DetailedResponse detailedResponse = new DetailedResponse();
        Map<Long, DetailedDTO> matchMap = new HashMap<>();
        Map<Long, DetailedDTO> playerMap = new HashMap<>();
        Set<Long> matchSet = new HashSet<>();
        Set<Long> playerSet = new HashSet<>();

        // Zpracování dat
        processDetail(detailList, filter, detailedResponse, matchMap, playerMap, matchSet, playerSet);

        // Nastavení odpovědi
        detailedResponse.setList(getSortedDetailList(filter, detailList, matchMap, playerMap));
        detailedResponse.setMatchesCount(matchSet.size());
        detailedResponse.setPlayersCount(playerSet.size());

        return detailedResponse;
    }

    private void processDetail(List<DetailedDTO> detailList, StatisticsFilter filter,
                              DetailedResponse detailedResponse,
                              Map<Long, DetailedDTO> matchMap,
                              Map<Long, DetailedDTO> playerMap,
                              Set<Long> matchSet, Set<Long> playerSet) {

        boolean matchStats = Boolean.TRUE.equals(filter.getMatchStatsOrPlayerStats());
        boolean playerStats = Boolean.FALSE.equals(filter.getMatchStatsOrPlayerStats());

        for (DetailedDTO detail : detailList) {
            detailedResponse.addTotal1(detail.getNumber1());
            detailedResponse.addTotal2(detail.getNumber2());

            matchSet.add(detail.getMatch().getId());
            playerSet.add(detail.getPlayer().getId());

            if (playerStats) {
                detail.setMatch(null);
                playerMap.compute(detail.getPlayer().getId(), (k, oldDetail) -> {
                    if (oldDetail == null) {
                        return detail;
                    } else {
                        oldDetail.addNumber1(detail.getNumber1());
                        oldDetail.addNumber2(detail.getNumber2());
                        return oldDetail;
                    }
                });
            }

            if (matchStats) {
                detail.setPlayer(null);
                matchMap.compute(detail.getMatch().getId(), (k, oldDetail) -> {
                    if (oldDetail == null) {
                        return detail;
                    } else {
                        oldDetail.addNumber1(detail.getNumber1());
                        oldDetail.addNumber2(detail.getNumber2());
                        return oldDetail;
                    }
                });
            }
        }
    }

    private List<DetailedDTO> getSortedDetailList(StatisticsFilter filter,
                                                    List<DetailedDTO> detailList,
                                                    Map<Long, DetailedDTO> matchMap,
                                                    Map<Long, DetailedDTO> playerMap) {
        List<DetailedDTO> resultList;

        if (Boolean.TRUE.equals(filter.getMatchStatsOrPlayerStats())) {
            resultList = new ArrayList<>(matchMap.values());
        } else if (Boolean.FALSE.equals(filter.getMatchStatsOrPlayerStats())) {
            resultList = new ArrayList<>(playerMap.values());
        } else {
            resultList = new ArrayList<>(detailList);
        }

        resultList.sort(new OrderDetailedDTOByFirstNumber());
        return resultList;
    }

    private List<DetailedDTO> getFilteredDetails(StatisticsFilter filter, DetailedType detailedType) {
        if (detailedType.equals(DetailedType.GOAL)) {
            return getFilteredGoals(filter).stream().map(DetailedDTO::new).toList();
        }
        else if (detailedType.equals(DetailedType.BEER)) {
            return getFilteredBeers(filter).stream().map(DetailedDTO::new).toList();
        }
        return new ArrayList<>();
    }

    private List<GoalDetailedDTO> getFilteredGoals(StatisticsFilter filter) {
        GoalStatsSpecification goalSpecification = new GoalStatsSpecification(filter);
        return goalRepository.findAll(goalSpecification, PageRequest.of(0, filter.getLimit()))
                .stream()
                .map(goalDetailedMapper::toDTO)
                .toList();
    }

    private List<BeerDetailedDTO> getFilteredBeers(StatisticsFilter filter) {
        BeerStatsSpecification beerSpecification = new BeerStatsSpecification(filter);
        return beerRepository.findAll(beerSpecification, PageRequest.of(0, filter.getLimit()))
                .stream()
                .map(beerDetailedMapper::toDTO)
                .toList();
    }

    public enum DetailedType {
        BEER,
        GOAL;
    }
}
