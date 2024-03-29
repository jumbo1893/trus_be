package com.jumbo.trus.entity.filter;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class BaseSeasonFilter extends BaseFilter {

    private Long seasonId;

    public BaseSeasonFilter(Long playerId, Long matchId) {
        super(playerId, matchId);
    }

    public BaseSeasonFilter(Long playerId, Long matchId, Long seasonId) {
        super(playerId, matchId);
        this.seasonId = seasonId;
    }

    public BaseSeasonFilter(Long matchId) {
        super(matchId);
    }
}
