package com.jumbo.trus.dto.receivedfine.response.stats.match;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchFineCountDTO {

    private StatsMatchDTO match;

    private long count;

    private long totalAmount;
}