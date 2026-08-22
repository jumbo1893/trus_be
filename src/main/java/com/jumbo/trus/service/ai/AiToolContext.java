package com.jumbo.trus.service.ai;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;

public record AiToolContext(
        UserEntity user,
        AppTeamEntity appTeam,
        Long currentPlayerId,
        String currentPlayerName,
        boolean aiExpertAccomplished
) {
    public AiToolContext(
            UserEntity user,
            AppTeamEntity appTeam,
            Long currentPlayerId,
            String currentPlayerName
    ) {
        this(user, appTeam, currentPlayerId, currentPlayerName, false);
    }
}
