package com.jumbo.trus.dto.beer.multi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.PlayerDTO;
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

    @NotNull
    private int beerNumber;

    @NotNull
    private int liquorNumber;

    @NotNull
    private PlayerDTO player;

    public BeerNoMatchWithPlayerDTO(@NotNull int beerNumber, @NotNull int liquorNumber, @NotNull PlayerDTO player) {
        this.beerNumber = beerNumber;
        this.liquorNumber = liquorNumber;
        this.player = player;
    }
}
