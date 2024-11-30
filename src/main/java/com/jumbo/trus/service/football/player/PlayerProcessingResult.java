package com.jumbo.trus.service.football.player;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PlayerProcessingResult {
    private final int totalPlayers;
    private final int updatedPlayers;
}
