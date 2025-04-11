package com.jumbo.trus.dto.beer.multi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.player.PlayerDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerNoMatchWithPlayerDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;

    private int beerNumber;

    private int liquorNumber;

    private PlayerDTO player;

    public BeerNoMatchWithPlayerDTO(int beerNumber, int liquorNumber, @NotNull PlayerDTO player) {
        this.beerNumber = beerNumber;
        this.liquorNumber = liquorNumber;
        this.player = player;
    }
}
