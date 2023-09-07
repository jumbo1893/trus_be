package com.jumbo.trus.dto.beer.response.get;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.multi.BeerNoMatchWithPlayerDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerSetupResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    @NotNull
    private SeasonDTO season;

    @NotNull
    private List<BeerNoMatchWithPlayerDTO> beerList;

    @NotNull
    private List<MatchDTO> matchList;

}
