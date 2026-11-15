package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.stats.MatchStatsDTO;
import com.jumbo.trus.service.fine.FineCodes;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Calculates ratings in tenths so the published score has exactly one decimal place. */
@Component
public class MatchPlayerRatingCalculator {

    public Map<Long, Double> calculate(
            MatchDTO match,
            MatchStatsDTO stats,
            Map<Long, Long> achievementCounts
    ) {
        Map<Long, Integer> tenths = new HashMap<>();
        for (Long playerId : match.getPlayerIdList()) {
            if (playerId != null) {
                tenths.put(playerId, 60);
            }
        }

        if (match.getHomeGoalNumber() != null && match.getAwayGoalNumber() != null) {
            int scored = match.isHome() ? match.getHomeGoalNumber() : match.getAwayGoalNumber();
            int conceded = match.isHome() ? match.getAwayGoalNumber() : match.getHomeGoalNumber();
            tenths.replaceAll((playerId, score) -> score + scored - 2 * conceded);
        }

        if (stats.getGoals() != null && stats.getGoals().getGoalList() != null) {
            for (GoalDetailedDTO goal : stats.getGoals().getGoalList()) {
                if (goal.getPlayer() != null) {
                    add(tenths, goal.getPlayer().getId(),
                            goal.getGoalNumber() * 4 + goal.getAssistNumber() * 2);
                }
            }
        }

        if (stats.getBeers() != null && stats.getBeers().getBeerList() != null) {
            for (BeerDetailedDTO beer : stats.getBeers().getBeerList()) {
                if (beer.getPlayer() != null) {
                    add(tenths, beer.getPlayer().getId(),
                            beer.getBeerNumber() * 2 + beer.getLiquorNumber() * 3);
                }
            }
        }

        Map<Long, Integer> fineAmounts = new HashMap<>();
        Set<Long> thirdHalfPlayers = new HashSet<>();
        if (stats.getFines() != null && stats.getFines().getFineList() != null) {
            for (ReceivedFineDetailedDTO received : stats.getFines().getFineList()) {
                if (received.getPlayer() == null || received.getFine() == null
                        || received.getFineNumber() <= 0) {
                    continue;
                }
                long playerId = received.getPlayer().getId();
                if (!tenths.containsKey(playerId)) {
                    continue;
                }
                fineAmounts.merge(playerId, Math.max(0, received.getFineAmount()), Integer::sum);
                String code = received.getFine().getCode();
                if (FineCodes.YELLOW_CARD.equals(code)) {
                    add(tenths, playerId, -7 * received.getFineNumber());
                } else if (FineCodes.RED_CARD.equals(code)) {
                    add(tenths, playerId, -20 * received.getFineNumber());
                } else if (FineCodes.OVERKICK.equals(code)) {
                    add(tenths, playerId, -received.getFineNumber());
                } else if (FineCodes.THIRD_HALF.equals(code)) {
                    thirdHalfPlayers.add(playerId);
                }
            }
        }
        fineAmounts.forEach((playerId, amount) -> add(tenths, playerId, -(amount / 100)));
        thirdHalfPlayers.forEach(playerId -> add(tenths, playerId, -5));
        achievementCounts.forEach((playerId, count) ->
                add(tenths, playerId, Math.toIntExact(count * 5)));

        Map<Long, Double> ratings = new HashMap<>();
        tenths.forEach((playerId, score) -> ratings.put(playerId, score / 10.0));
        return ratings;
    }

    private void add(Map<Long, Integer> scores, long playerId, int amount) {
        scores.computeIfPresent(playerId, (id, value) -> value + amount);
    }
}
