package com.jumbo.trus.dto.goal.response.get;

import com.jumbo.trus.service.helper.DetailedResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalDetailedResponse {

    private int playersCount = 0;

    private int matchesCount = 0;

    private int totalGoals = 0;

    private int totalAssists = 0;

    @NotNull
    private List<GoalDetailedDTO> goalList;

    public void addGoals(int goals) {
        totalGoals+=goals;
    }

    public void addAssists(int assists) {
        totalAssists+=assists;
    }

    public GoalDetailedResponse (DetailedResponse detailedResponse) {
        this.playersCount = detailedResponse.getPlayersCount();
        this.matchesCount = detailedResponse.getMatchesCount();
        this.totalGoals = detailedResponse.getTotal1();
        this.totalAssists = detailedResponse.getTotal2();
        this.goalList = detailedResponse.getList()
                .stream()
                .map(GoalDetailedDTO::new)
                .collect(Collectors.toList());
    }
}
