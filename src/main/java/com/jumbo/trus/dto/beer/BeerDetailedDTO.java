package com.jumbo.trus.dto.beer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.MatchDTO;
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
    @JsonProperty("_id")
    private long id;

    @NotNull
    private int beerNumber;

    @NotNull
    private int liquorNumber;

    @NotNull
    private PlayerDTO player;

    @NotNull
    private MatchDTO match;
}
