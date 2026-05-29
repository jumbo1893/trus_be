package com.jumbo.trus.dto.receivedfine.response.stats.player;

import com.jumbo.trus.dto.receivedfine.response.stats.FineCountDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PlayerWithFinesDTO {

    private StatsPlayerDTO player;

    private long totalAmount;

    private long totalCount;

    private List<FineCountDTO> fines;
}