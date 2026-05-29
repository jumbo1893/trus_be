package com.jumbo.trus.dto.match.stats;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayersInMatchStats {

    private int playersCount = 0;

    private int matchesCount = 0;

    @NotNull
    private List<DetailMatchPlayer> detailMatchPlayers;

    public void addPlayer(int players) {
        playersCount+=players;
    }

    public void addMatches(int matches) {
        matchesCount+=matches;
    }


}
