package com.jumbo.trus.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDTO {

    private String dropdownText;

    private List<PlayerStatsDTO> playerStats;
}
