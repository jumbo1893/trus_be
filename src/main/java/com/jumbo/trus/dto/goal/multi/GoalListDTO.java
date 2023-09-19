package com.jumbo.trus.dto.goal.multi;

import com.jumbo.trus.dto.goal.GoalDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalListDTO {

    @NotNull
    private Long matchId;

    private boolean rewriteToFines;

    @NotNull
    private List<GoalDTO> goalList;



}
