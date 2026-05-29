package com.jumbo.trus.dto.match.stats;

import com.jumbo.trus.dto.player.PlayerDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayersInMatch {

    private Long matchId;

    private String matchName;

    @NotNull
    private List<PlayerDTO> playerList;

    @NotNull
    private List<PlayerDTO> fanList;

}
