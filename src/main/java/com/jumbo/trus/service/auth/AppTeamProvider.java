package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.AppTeamDTO;

import java.util.List;

public interface AppTeamProvider {
    List<AppTeamDTO> getAllAppTeams();
    AppTeamDTO getLisciTrusAppTeam();
}
