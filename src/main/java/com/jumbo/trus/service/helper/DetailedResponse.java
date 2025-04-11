package com.jumbo.trus.service.helper;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailedResponse {

    private int playersCount = 0;

    private int matchesCount = 0;

    private int total1 = 0;

    private int total2 = 0;

    @NotNull
    private List<DetailedDTO> list;

    public void addTotal1(int number) {
        total1+=number;
    }

    public void addTotal2(int number) {
        total2+=number;
    }

    public DetailedResponse(BeerDetailedResponse beerDetailedResponse) {
        this.playersCount = beerDetailedResponse.getPlayersCount();
        this.matchesCount = beerDetailedResponse.getMatchesCount();
        this.total1 = beerDetailedResponse.getTotalBeers();
        this.total2 = beerDetailedResponse.getTotalLiquors();
        this.list = beerDetailedResponse.getBeerList()
                .stream()
                .map(DetailedDTO::new)
                .collect(Collectors.toList());
    }

    public DetailedResponse(GoalDetailedResponse goalDetailedResponse) {
        this.playersCount = goalDetailedResponse.getPlayersCount();
        this.matchesCount = goalDetailedResponse.getMatchesCount();
        this.total1 = goalDetailedResponse.getTotalGoals();
        this.total2 = goalDetailedResponse.getTotalAssists();
        this.list = goalDetailedResponse.getGoalList()
                .stream()
                .map(DetailedDTO::new)
                .collect(Collectors.toList());
    }
}
