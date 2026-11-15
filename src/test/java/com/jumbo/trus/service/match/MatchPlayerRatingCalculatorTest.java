package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.dto.stats.MatchStatsDTO;
import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MatchPlayerRatingCalculatorTest {

    private final MatchPlayerRatingCalculator calculator = new MatchPlayerRatingCalculator();

    @Test
    void appliesEveryRuleAndRoundsFineAmountAfterSumming() {
        MatchDTO match = new MatchDTO();
        match.setPlayerIdList(List.of(1L, 2L));
        match.setHome(true);
        match.setHomeGoalNumber(3);
        match.setAwayGoalNumber(1);

        PlayerDTO player = new PlayerDTO();
        player.setId(1L);
        GoalDetailedDTO goal = new GoalDetailedDTO();
        goal.setPlayer(player);
        goal.setGoalNumber(2);
        goal.setAssistNumber(1);
        GoalDetailedResponse goals = new GoalDetailedResponse();
        goals.setGoalList(List.of(goal));

        BeerDetailedDTO beer = new BeerDetailedDTO();
        beer.setPlayer(player);
        beer.setBeerNumber(2);
        beer.setLiquorNumber(1);
        BeerDetailedResponse beers = new BeerDetailedResponse();
        beers.setBeerList(List.of(beer));

        ReceivedFineDetailedResponse fines = new ReceivedFineDetailedResponse();
        fines.setFineList(List.of(
                fine(player, FineCodes.YELLOW_CARD, 1, 0),
                fine(player, FineCodes.RED_CARD, 1, 0),
                fine(player, FineCodes.THIRD_HALF, 1, 0),
                fine(player, FineCodes.OVERKICK, 2, 0),
                fine(player, "CUSTOM_ONE", 1, 350),
                fine(player, "CUSTOM_TWO", 1, 400)
        ));

        MatchStatsDTO stats = new MatchStatsDTO();
        stats.setGoals(goals);
        stats.setBeers(beers);
        stats.setFines(fines);

        Map<Long, Double> ratings = calculator.calculate(match, stats, Map.of(1L, 2L));

        assertThat(ratings).containsEntry(1L, 4.7).containsEntry(2L, 6.1);
    }

    @Test
    void ignoresMissingScoreAndUnlistedPlayers() {
        MatchDTO match = new MatchDTO();
        match.setPlayerIdList(List.of(1L));
        MatchStatsDTO stats = new MatchStatsDTO();
        GoalDetailedDTO goal = new GoalDetailedDTO();
        PlayerDTO other = new PlayerDTO();
        other.setId(2L);
        goal.setPlayer(other);
        goal.setGoalNumber(10);
        GoalDetailedResponse goals = new GoalDetailedResponse();
        goals.setGoalList(List.of(goal));
        stats.setGoals(goals);

        assertThat(calculator.calculate(match, stats, Map.of(2L, 3L)))
                .containsOnlyKeys(1L)
                .containsEntry(1L, 6.0);
    }

    private ReceivedFineDetailedDTO fine(PlayerDTO player, String code, int count, int amount) {
        FineDTO fine = new FineDTO();
        fine.setCode(code);
        ReceivedFineDetailedDTO received = new ReceivedFineDetailedDTO();
        received.setPlayer(player);
        received.setFine(fine);
        received.setFineNumber(count);
        received.setFineAmount(amount);
        return received;
    }
}
