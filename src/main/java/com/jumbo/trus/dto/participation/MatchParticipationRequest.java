package com.jumbo.trus.dto.participation;

import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchParticipationRequest {

    @NotNull
    private Long footballMatchId;

    private Long playerId;

    @NotNull
    private MatchParticipationStatus status;

    @Size(max = 1000)
    private String comment;
}
