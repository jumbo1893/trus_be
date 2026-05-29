package com.jumbo.trus.dto.receivedfine.response.stats.player;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerFineCountDTO {

    private StatsPlayerDTO player;

    private long count;

    private long totalAmount;
}