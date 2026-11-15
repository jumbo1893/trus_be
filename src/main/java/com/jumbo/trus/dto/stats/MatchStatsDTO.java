package com.jumbo.trus.dto.stats;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchStatsDTO {

    private GoalDetailedResponse goals;

    private BeerDetailedResponse beers;

    private ReceivedFineDetailedResponse fines;

    private Map<Long, Double> playerRatings = new HashMap<>();

    private Map<Long, List<String>> playerAchievements = new HashMap<>();

    private RefereeStatsDTO refereeStats;
}
