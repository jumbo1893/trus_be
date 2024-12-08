package com.jumbo.trus.service.football.match;

import com.jumbo.trus.service.helper.Pair;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MatchResultCounter {
    private int fullyProcessedMatches;
    private int partiallyProcessedMatches;
    private int  unprocessedMatches;
    private final List<Long> processedMatchIds;

    public MatchResultCounter() {
        this.processedMatchIds = new ArrayList<>();
        this.fullyProcessedMatches = 0;
        this.partiallyProcessedMatches = 0;
        this.unprocessedMatches = 0;
    }

    public void addCounts( Pair<MatchProcessingResult, Long> result) {
        MatchProcessingResult processType = result.getFirst();
        processedMatchIds.add(result.getSecond());
        switch (processType) {
            case FULLY_PROCESSED -> fullyProcessedMatches++;
            case PARTIALLY_PROCESSED -> partiallyProcessedMatches++;
            case UNPROCESSED -> unprocessedMatches++;
        }
    }

}
