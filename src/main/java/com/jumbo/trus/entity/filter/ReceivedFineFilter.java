package com.jumbo.trus.entity.filter;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ReceivedFineFilter extends BaseSeasonFilter {


    private int fineNumber;

    private Long fineId;

    //defaultní hodnota
    private int limit = 1000;

    public ReceivedFineFilter(Long matchId, Long playerId, Long fineId) {
        super(playerId, matchId);
        this.fineId = fineId;
    }

    public ReceivedFineFilter(Long matchId, Long playerId) {
        super(playerId, matchId);
    }
}
