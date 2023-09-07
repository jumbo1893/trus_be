package com.jumbo.trus.dto.beer.response.get;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.PlayerDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerDetailedDTO {

    @NotNull
    private long id;

    @NotNull
    private int beerNumber;

    @NotNull
    private int liquorNumber;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PlayerDTO player;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    public void addBeers(int beers) {
        beerNumber+=beers;
    }

    public void addLiquors(int liquors) {
        liquorNumber+=liquors;
    }
}
