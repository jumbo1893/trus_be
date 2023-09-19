package com.jumbo.trus.dto.goal.response.get;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
}
