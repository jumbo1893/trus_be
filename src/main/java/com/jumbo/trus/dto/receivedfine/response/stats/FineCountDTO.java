package com.jumbo.trus.dto.receivedfine.response.stats;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FineCountDTO {

    private FineStatsDTO fine;

    private long count;

    private long totalAmount;
}