package com.jumbo.trus.dto.goal.response;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalMultiAddResponse {

    private int totalGoalsAdded = 0;

    private int totalAssistAdded = 0;

    @NotNull
    private String match = "";

    public void addGoals(int goals) {
        totalGoalsAdded+=goals;
    }

    public void addAssists(int assists) {
        totalAssistAdded+=assists;
    }

}
