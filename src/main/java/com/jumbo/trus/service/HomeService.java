package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.UserDTO;
import com.jumbo.trus.dto.home.Chart;
import com.jumbo.trus.dto.home.Coordinate;
import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.helper.RandomFact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HomeService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private BeerService beerService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private ReceivedFineService receivedFineService;

    @Autowired
    private RandomFact randomFact;

    @Autowired
    private UserService userService;


    public HomeSetup setup(Long playerId) {
        HomeSetup homeSetup = new HomeSetup();
        homeSetup.setNextBirthday(getUpcomingBirthday());
        homeSetup.setRandomFacts(randomFact.getRandomFacts());
        homeSetup.setChart(setupChartCoordinates(playerId));
        return homeSetup;
    }

    private Chart setupChartCoordinates(Long playerId) {
        UserDTO userDTO;
        if (playerId != null) {
            userDTO = new UserDTO();
            userDTO.setPlayerId(playerId);
        }
        else {
            userDTO = userService.getCurrentUser();
            if (userDTO.getPlayerId() == null) {
                return null;
            }
        }
        List<Coordinate> coordinates = new ArrayList<>();
        List<MatchDTO> matches = matchService.getMatchesByDate(5, true);
        Collections.reverse(matches);
        int beerMaximum = 0;
        int fineMaximum = 0;
        for (MatchDTO match : matches) {
            Coordinate coordinate = new Coordinate();
            coordinate.setMatchInitials(getMatchInitials(match));
            StatisticsFilter statisticsFilter = new StatisticsFilter(userDTO.getPlayerId(), match.getId(), Config.ALL_SEASON_ID, false);
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
        return new Chart(fineMaximum, beerMaximum, getBeerChartLabels(beerMaximum), getFineChartLabels(fineMaximum), coordinates);
    }

    private String getMatchInitials(MatchDTO matchDTO) {
        String nameWithoutSpaces = matchDTO.getName().replaceAll(" ", "").toUpperCase();
        if (nameWithoutSpaces.length() >= 3) {
            return nameWithoutSpaces.substring(0,3);
        }
        else if (nameWithoutSpaces.length() == 2) {
            return nameWithoutSpaces.substring(0, 2);
        }
       return nameWithoutSpaces.substring(0,1);
    }

    private List<Integer> getBeerChartLabels(int maximum) {
        List<Integer> labels = new ArrayList<>();
        for (int i = 0; i < maximum; i+=5) {
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
        if (number/100 <= 4) {
            for (int i = 0; i < number; i += 100) {
                labels.add(i*multiple);
            }
            return labels;
        }
        return calculateFineLabels(roundNumberUp(number/2, 100), multiple*2);
    }

    private int roundNumberUp(int number, int divisor) {
        return (number + divisor - 1) / divisor * divisor;
    }

    private int roundNumber(int number, int numberToRound, int minimum) {
        int remainder = number % numberToRound;
        return (remainder > 0) ? number + (numberToRound - remainder) : minimum;
    }

    private String getUpcomingBirthday() {
        return playerService.returnNextPlayerBirthdayFromList();
    }

}
