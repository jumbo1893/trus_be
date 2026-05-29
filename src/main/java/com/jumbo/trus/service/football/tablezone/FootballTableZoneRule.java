package com.jumbo.trus.service.football.tablezone;

import com.jumbo.trus.dto.football.FootballTableZone;
import lombok.Getter;

@Getter
public class FootballTableZoneRule {

    private final Integer rankFrom;
    private final Integer rankTo;
    private final Integer bottomCount;
    private final FootballTableZone zone;

    private FootballTableZoneRule(
            Integer rankFrom,
            Integer rankTo,
            Integer bottomCount,
            FootballTableZone zone
    ) {
        this.rankFrom = rankFrom;
        this.rankTo = rankTo;
        this.bottomCount = bottomCount;
        this.zone = zone;
    }

    public static FootballTableZoneRule rank(
            int rank,
            FootballTableZone zone
    ) {
        return new FootballTableZoneRule(rank, rank, null, zone);
    }

    public static FootballTableZoneRule range(
            int rankFrom,
            int rankTo,
            FootballTableZone zone
    ) {
        return new FootballTableZoneRule(rankFrom, rankTo, null, zone);
    }

    public static FootballTableZoneRule bottom(
            int bottomCount,
            FootballTableZone zone
    ) {
        return new FootballTableZoneRule(null, null, bottomCount, zone);
    }

    public boolean appliesTo(int rank, int tableSize) {
        if (bottomCount != null) {
            return rank > tableSize - bottomCount;
        }

        return rank >= rankFrom && rank <= rankTo;
    }
}