package com.jumbo.trus.dto.participation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchParticipationCommentRequest {

    @NotNull
    private Long footballMatchId;

    @NotBlank
    @Size(max = 1000)
    private String text;

    private Long parentCommentId;
}
