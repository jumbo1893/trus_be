package com.jumbo.trus.dto.beer;

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

    private int beerNumber;

    private int liquorNumber;

    @NotNull
    private Long playerId;

    @NotNull
    private Long matchId;

    public BeerDTO(@NotNull Long matchId, BeerNoMatchDTO beerNoMatchDTO) {
        this.matchId = matchId;
        this.id = beerNoMatchDTO.getId();
        this.beerNumber = beerNoMatchDTO.getBeerNumber();
        this.liquorNumber = beerNoMatchDTO.getLiquorNumber();
        this.playerId = beerNoMatchDTO.getPlayerId();
    }

    public BeerDTO(int beerNumber, int liquorNumber) {
        this.beerNumber = beerNumber;
        this.liquorNumber = liquorNumber;
    }
}
