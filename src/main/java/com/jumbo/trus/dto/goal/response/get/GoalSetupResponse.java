package com.jumbo.trus.dto.goal.response.get;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.PlayerDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalSetupResponse {

    @JsonProperty("id")
    private long id;

    private int goalNumber;
    private int assistNumber;

    @NotNull
    private PlayerDTO player;

    public GoalSetupResponse(int goalNumber, int assistNumber, @NotNull PlayerDTO player) {
        this.goalNumber = goalNumber;
        this.assistNumber = assistNumber;
        this.player = player;
    }

    public GoalSetupResponse newGoalSetup(PlayerDTO player) {
        return new GoalSetupResponse(0, 0, player);
    }


}
