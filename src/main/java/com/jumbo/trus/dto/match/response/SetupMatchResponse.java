package com.jumbo.trus.dto.match.response;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetupMatchResponse {

    private MatchDTO match;

    @NotNull
    private List<SeasonDTO> seasonList;

    @NotNull
    private List<PlayerDTO> playerList;

    @NotNull
    private List<PlayerDTO> fanList;

    @NotNull
    private SeasonDTO primarySeason;

    private FootballMatchDTO footballMatch;

}
