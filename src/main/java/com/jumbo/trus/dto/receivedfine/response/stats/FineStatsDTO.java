package com.jumbo.trus.dto.receivedfine.response.stats;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FineStatsDTO {

    private Long id;

    private String name;

    private int amount;
}