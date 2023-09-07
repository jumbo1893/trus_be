package com.jumbo.trus.service.fact;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import lombok.Data;

import java.util.List;

@Data
public class FineFact {

    final ReceivedFineDetailedResponse allFineDetailedResponseForMatch;
    final ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch;

    final ReceivedFineDetailedResponse allFineDetailedResponseForPlayer;
    final ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForPlayer;


    public FineFact(ReceivedFineDetailedResponse allFineDetailedResponseForMatch, ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch,
                    ReceivedFineDetailedResponse allFineDetailedResponseForPlayer, ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForPlayer) {
        this.allFineDetailedResponseForMatch = allFineDetailedResponseForMatch;
        this.currentSeasonFineDetailedResponseForMatch = currentSeasonFineDetailedResponseForMatch;
        this.allFineDetailedResponseForPlayer = allFineDetailedResponseForPlayer;
        this.currentSeasonFineDetailedResponseForPlayer = currentSeasonFineDetailedResponseForPlayer;
    }
}
