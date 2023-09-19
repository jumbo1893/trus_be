package com.jumbo.trus.dto.beer.response.get;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
}
