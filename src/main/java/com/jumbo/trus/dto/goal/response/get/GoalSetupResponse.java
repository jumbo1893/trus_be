package com.jumbo.trus.dto.goal.response.get;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalSetupResponse {

    @NotNull
    @JsonProperty("id")
    private long id;

    @NotNull
    private int goalNumber;

    @NotNull
    private int assistNumber;

    @NotNull
    private PlayerDTO player;

    public GoalSetupResponse(int goalNumber, int assistNumber, PlayerDTO player) {
        this.goalNumber = goalNumber;
        this.assistNumber = assistNumber;
        this.player = player;
    }

    public GoalSetupResponse newGoalSetup(PlayerDTO player) {
        return new GoalSetupResponse(0, 0, player);
    }


}
