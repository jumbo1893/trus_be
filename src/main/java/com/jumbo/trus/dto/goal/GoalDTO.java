package com.jumbo.trus.dto.goal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.beer.multi.BeerNoMatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalDTO {

    @NotNull
    private long id;

    @NotNull
    private int goalNumber;

    @NotNull
    private int assistNumber;

    @NotNull
    private Long playerId;

    @NotNull
    private Long matchId;

}
