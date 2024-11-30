package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.Organization;
import com.jumbo.trus.service.UpdateService;
import com.jumbo.trus.service.football.helper.TeamTableTeam;
import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflTeams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamRetriever {

    private final RetrievePkflTeams retrievePkflTeams;
    private final UpdateService updateService;
    private final LeagueService leagueService;

    private static final String TEAM_UPDATE = "team_update";

    public List<TeamTableTeam> retrieveTeams(boolean loadAllTeams) {
        if (loadAllTeams) {
            return retrieveAllTeams();
        }
        return retrieveCurrentLeagueTeams();
    }

    private List<TeamTableTeam> retrieveAllTeams() {
        List<TeamTableTeam> teams = retrievePkflTeams.getTeams(leagueService.getAllLeagues(Organization.PKFL));
        if (!teams.isEmpty()) {
            updateService.saveNewUpdate(TEAM_UPDATE);
        }
        return teams;
    }

    private List<TeamTableTeam> retrieveCurrentLeagueTeams() {
        return retrievePkflTeams.getTeams(leagueService.getAllLeagues(Organization.PKFL, true));
    }

    public boolean isUpdateNeeded() {
        return updateService.getUpdateByName(TEAM_UPDATE) == null;
    }
}
