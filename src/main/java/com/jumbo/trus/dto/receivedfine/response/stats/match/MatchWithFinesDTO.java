package com.jumbo.trus.dto.receivedfine.response.stats.match;

import com.jumbo.trus.dto.receivedfine.response.stats.FineCountDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MatchWithFinesDTO {

    private StatsMatchDTO match;

    private long totalAmount;

    private long totalCount;

    private List<FineCountDTO> fines;
}