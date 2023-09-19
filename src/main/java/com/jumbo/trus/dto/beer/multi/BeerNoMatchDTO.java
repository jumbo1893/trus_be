package com.jumbo.trus.dto.beer.multi;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerNoMatchDTO {


    private long id;

    private int beerNumber;

    private int liquorNumber;

    @NotNull
    private Long playerId;
}
