package com.jumbo.trus.dto.beer.response.get;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.service.helper.DetailedDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerDetailedDTO {

    private long id;

    private int beerNumber;

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

    public BeerDetailedDTO(DetailedDTO detailedDTO) {
        this.id = detailedDTO.getId();
        this.beerNumber = detailedDTO.getNumber1();
        this.liquorNumber = detailedDTO.getNumber2();
        this.player = detailedDTO.getPlayer();
        this.match = detailedDTO.getMatch();
    }
}
