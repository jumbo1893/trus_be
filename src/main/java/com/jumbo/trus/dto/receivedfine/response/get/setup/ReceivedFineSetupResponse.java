package com.jumbo.trus.dto.receivedfine.response.get.setup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.response.stats.player.PlayerWithFinesDTO;
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


    @NotNull
    private List<PlayerWithFinesDTO> playerFineSummaries;

    public ReceivedFineSetupResponse(
            MatchDTO match,
            @NotNull SeasonDTO season,
            @NotNull List<PlayerDTO> playersInMatch,
            @NotNull List<PlayerDTO> otherPlayers,
            @NotNull List<MatchDTO> matchList
    ) {
        this.match = match;
        this.season = season;
        this.playersInMatch = playersInMatch;
        this.otherPlayers = otherPlayers;
        this.matchList = matchList;
        this.playerFineSummaries = List.of();
    }
}