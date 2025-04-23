package com.jumbo.trus.dto.auth.registration;

import com.jumbo.trus.dto.auth.AppTeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationSetup {

    private List<LeagueWithTeams> leagueWithTeamsList;

    private LeagueWithTeams primaryLeague;

    private TeamWithAppTeams primaryTeam;

    private AppTeamDTO primaryAppTeam;

}
