package com.jumbo.trus.dto.beer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.beer.multi.BeerNoMatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerDTO {

    private long id;

    @NotNull
    private int beerNumber;

    @NotNull
    private int liquorNumber;

    @NotNull
    private Long playerId;

    @NotNull
    private Long matchId;

    public BeerDTO(Long matchId, BeerNoMatchDTO beerNoMatchDTO) {
        this.matchId = matchId;
        this.id = beerNoMatchDTO.getId();
        this.beerNumber = beerNoMatchDTO.getBeerNumber();
        this.liquorNumber = beerNoMatchDTO.getLiquorNumber();
        this.playerId = beerNoMatchDTO.getPlayerId();
    }
}
