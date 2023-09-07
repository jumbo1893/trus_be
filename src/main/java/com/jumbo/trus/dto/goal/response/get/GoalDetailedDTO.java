package com.jumbo.trus.dto.goal.response.get;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalDetailedDTO {

    @NotNull
    private long id;

    @NotNull
    private int goalNumber;

    @NotNull
    private int assistNumber;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PlayerDTO player;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    public void addGoals(int goals) {
        goalNumber+=goals;
    }

    public void addAssists(int assists) {
        assistNumber+=assists;
    }
}
