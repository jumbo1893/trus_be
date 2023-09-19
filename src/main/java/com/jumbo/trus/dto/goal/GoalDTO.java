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

    @NotNull
    private Long playerId;

    @NotNull
    private Long matchId;

}
