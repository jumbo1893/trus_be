package com.jumbo.trus.dto.beer.response.get;

import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.service.helper.DetailedResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerDetailedResponse {

    private int playersCount = 0;

    private int matchesCount = 0;

    private int totalBeers = 0;

    private int totalLiquors = 0;

    @NotNull
    private List<BeerDetailedDTO> beerList;

    public void addBeers(int beers) {
        totalBeers+=beers;
    }

    public void addLiquors(int liquors) {
        totalLiquors+=liquors;
    }

    public BeerDetailedResponse(DetailedResponse detailedResponse) {
        this.playersCount = detailedResponse.getPlayersCount();
        this.matchesCount = detailedResponse.getMatchesCount();
        this.totalBeers = detailedResponse.getTotal1();
        this.totalLiquors = detailedResponse.getTotal2();
        this.beerList = detailedResponse.getList()
                .stream()
                .map(BeerDetailedDTO::new)
                .collect(Collectors.toList());
    }
}
