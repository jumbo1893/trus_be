package com.jumbo.trus.dto.receivedfine.response.get.setup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineSetupResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    @NotNull
    private SeasonDTO season;

    @NotNull
    private List<PlayerDTO> playersInMatch;

    @NotNull
    private List<PlayerDTO> otherPlayers;

    @NotNull
    private List<MatchDTO> matchList;

}
