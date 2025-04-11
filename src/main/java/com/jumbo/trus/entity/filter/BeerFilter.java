package com.jumbo.trus.entity.filter;


import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class BeerFilter extends BaseSeasonFilter {


    private int beerNumber;

    private int liquorNumber;

    public BeerFilter(Long matchId, Long playerId) {
        super(playerId, matchId);
    }

    public BeerFilter(Long matchId) {
        super(matchId);
    }
}
