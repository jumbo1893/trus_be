package com.jumbo.trus.dto.receivedfine.response.stats.player;

import com.jumbo.trus.dto.receivedfine.response.stats.FineStatsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FineWithPlayersDTO {

    private FineStatsDTO fine;

    private long totalAmount;

    private long totalCount;

    private List<PlayerFineCountDTO> players;
}