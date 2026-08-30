package com.jumbo.trus.dto.participation;

import com.jumbo.trus.entity.participation.MatchParticipationPromptAudience;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchParticipationPromptAudienceConfig {

    @NotNull
    private MatchParticipationPromptAudience audience;
}
