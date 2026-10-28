package com.jumbo.trus.dto.goal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalDTO {

    private long id;

    private int goalNumber;

    private int assistNumber;

    private int yellowCards;
    private int redCards;
    private int goalkeepingMinutes;

    @NotNull
    private Long playerId;

    @NotNull
    private Long matchId;

}
