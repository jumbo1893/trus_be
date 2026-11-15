package com.jumbo.trus.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefereeStatsDTO {
    private String name;
    private long matchCount;
    private double winPercentage;
    private String mostStarredPlayer;
    private long mostStarredCount;
    private double yellowCardsPerMatch;
    private double redCardsPerMatch;
}
