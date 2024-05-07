package com.jumbo.trus.service.home;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.UserDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.home.Chart;
import com.jumbo.trus.dto.home.Coordinate;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.*;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.beer.BeerStatsService;
import com.jumbo.trus.service.beer.helper.AverageBeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChartMaker {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private BeerService beerService;

    @Autowired
    private BeerStatsService beerStatsService;

    @Autowired
    private SeasonService seasonService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private ReceivedFineService receivedFineService;

    @Autowired
    private UserService userService;


    public Chart setupChartCoordinatesForUser(Long playerId) {
        final Long finalPlayerId = getPlayerId(playerId);
        if (finalPlayerId == null) {
            return null;
        }
        List<MatchDTO> matches = matchService.getMatchesByDate(5, true);
        Collections.reverse(matches);
        return getPlayerChart(finalPlayerId, matches, finalPlayerId);
    }

    public List<Chart> setupChartsCoordinates(Long playerId) {
        final Long finalPlayerId = getPlayerId(playerId);
        if (finalPlayerId == null) {
            return null;
        }
        List<Long> surroundingPlayerIds = findSurroundingPlayers(finalPlayerId);
        List<Chart> surroundingPlayerCharts = new ArrayList<>();
        List<MatchDTO> matches = matchService.getMatchesByDate(5, true);
        Collections.reverse(matches);
        for (Long surroundingPlayerId : surroundingPlayerIds) {
            surroundingPlayerCharts.add(getPlayerChart(surroundingPlayerId, matches, finalPlayerId));
        }
        return surroundingPlayerCharts;
    }

    private Long getPlayerId(Long playerId) {
        UserDTO userDTO;
        if (playerId != null) {
            userDTO = new UserDTO();
            userDTO.setPlayerId(playerId);
        } else {
            userDTO = userService.getCurrentUser();
            if (userDTO.getPlayerId() == null) {
                return null;
            }
        }
        return userDTO.getPlayerId();
    }

    private Chart getPlayerChart(Long playerId, List<MatchDTO> matches, Long originalPlayerId) {
        List<Coordinate> coordinates = new ArrayList<>();
        int beerMaximum = 0;
        int fineMaximum = 0;
        for (MatchDTO match : matches) {
            Coordinate coordinate = new Coordinate();
            coordinate.setMatchInitials(getMatchInitials(match));
            StatisticsFilter statisticsFilter = new StatisticsFilter(playerId, match.getId(), Config.ALL_SEASON_ID, false);
            BeerDetailedResponse beer = beerService.getAllDetailed(statisticsFilter);
            ReceivedFineDetailedResponse fine = receivedFineService.getAllDetailed(statisticsFilter);
            if (beer.getTotalBeers() > beerMaximum) {
                beerMaximum = beer.getTotalBeers();
            }
            coordinate.setBeerNumber(beer.getTotalBeers());
            if (beer.getTotalLiquors() > beerMaximum) {
                beerMaximum = beer.getTotalLiquors();
            }
            coordinate.setLiquorNumber(beer.getTotalLiquors());
            if (fine.getFinesAmount() > fineMaximum) {
                fineMaximum = fine.getFinesAmount();
            }
            coordinate.setFineAmount(fine.getFinesAmount());
            coordinates.add(coordinate);
        }
        beerMaximum = roundNumber(beerMaximum, 5, 10);
        fineMaximum = roundNumber(fineMaximum, 100, 100);
        return new Chart(fineMaximum, beerMaximum, getBeerChartLabels(beerMaximum), getFineChartLabels(fineMaximum), coordinates, playerService.getPlayer(playerId), playerId.equals(originalPlayerId));
    }

    private List<Long> findSurroundingPlayers(Long playerId) {
        List<AverageBeer> sumBeerList = beerStatsService.getAverageNumberList(true, seasonService.getCurrentSeason(false).getId(), "totalBeerNumber");
        if (sumBeerList.size() < 5) {
            sumBeerList = beerStatsService.getAverageNumberList(true, Config.ALL_SEASON_ID, "totalBeerNumber");
        }
        if (sumBeerList.size() < 5) {
            return getPlayerIdsFromBeerList(sumBeerList);
        }
        return getPlayerIdsFromBeerList(getSurroundingBeersFromBeerList(sumBeerList, playerId));
    }

    private List<Long> getPlayerIdsFromBeerList(List<AverageBeer> beerList) {
        List<Long> playerIds = new ArrayList<>();
        for (AverageBeer beer : beerList) {
            playerIds.add(beer.getPlayerId());
        }
        return playerIds;
    }

    private List<AverageBeer> getSurroundingBeersFromBeerList(List<AverageBeer> beerList, Long originalPlayerId) {
        List<AverageBeer> surroundingBeerList = new ArrayList<>();
        int originalIndex = -1;
        for (AverageBeer beer : beerList) {
            if (originalPlayerId.equals(beer.getPlayerId())) {
                originalIndex = beerList.indexOf(beer);
                break;
            }
        }
        if (originalIndex <= 2) {
            for (int i = 0; i < 5; i++) {
                surroundingBeerList.add(beerList.get(i));
            }
            return surroundingBeerList;
        } else if (originalIndex >= beerList.size() - 3) {
            for (int i = beerList.size() - 1; i > beerList.size() - 6; i--) {
                surroundingBeerList.add(beerList.get(i));
            }
            return surroundingBeerList;
        }
        for (int i = originalIndex - 2; i <= originalIndex + 2; i++) {
            surroundingBeerList.add(beerList.get(i));
        }
        return surroundingBeerList;
    }

    private String getMatchInitials(MatchDTO matchDTO) {
        String nameWithoutSpaces = matchDTO.getName().replaceAll(" ", "").toUpperCase();
        if (nameWithoutSpaces.length() >= 3) {
            return nameWithoutSpaces.substring(0, 3);
        } else if (nameWithoutSpaces.length() == 2) {
            return nameWithoutSpaces.substring(0, 2);
        }
        return nameWithoutSpaces.substring(0, 1);
    }

    private List<Integer> getBeerChartLabels(int maximum) {
        List<Integer> labels = new ArrayList<>();
        for (int i = 0; i < maximum; i += 5) {
            labels.add(i);
        }
        return labels;
    }

    private List<Integer> getFineChartLabels(int maximum) {
        List<Integer> labels = new ArrayList<>();
        if (maximum <= 200) {
            for (int i = 0; i < maximum; i += 50) {
                labels.add(i);
            }
            return labels;
        }
        return calculateFineLabels(maximum, 1);
    }

    private List<Integer> calculateFineLabels(int number, int multiple) {
        List<Integer> labels = new ArrayList<>();
        if (number / 100 <= 4) {
            for (int i = 0; i < number; i += 100) {
                labels.add(i * multiple);
            }
            return labels;
        }
        return calculateFineLabels(roundNumberUp(number / 2, 100), multiple * 2);
    }

    private int roundNumberUp(int number, int divisor) {
        return (number + divisor - 1) / divisor * divisor;
    }

    private int roundNumber(int number, int numberToRound, int minimum) {
        int remainder = number % numberToRound;
        return (remainder > 0) ? number + (numberToRound - remainder) : minimum;
    }
}
