package com.jumbo.trus.dto.beer.multi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerNoMatchDTO {


    private long id;

    @NotNull
    private int beerNumber;

    @NotNull
    private int liquorNumber;

    @NotNull
    private Long playerId;
}
