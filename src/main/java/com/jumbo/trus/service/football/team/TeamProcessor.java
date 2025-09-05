package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.repository.football.TeamRepository;
import com.jumbo.trus.mapper.football.TeamMapper;
import com.jumbo.trus.service.football.helper.TeamTableTeam;
import com.jumbo.trus.service.helper.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamProcessor {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final TableTeamProcessor tableTeamProcessor;

    public List<TableTeamDTO> getTableTeamsByTeamId(Long teamId) {
        TeamDTO teamDTO = getTeamById(teamId);
        if (teamDTO == null) {
            return new ArrayList<>();
        }
        return tableTeamProcessor.getTableTeamsByTeam(teamDTO);
    }

    public TeamDTO getTeamById(Long teamId) {
        return teamMapper.toDTO(teamRepository.findById(teamId).orElse(null));
    }

    public boolean isNewTeam(TeamDTO team) {
        return !teamRepository.existsByUri(team.getUri());
    }

    public TeamDTO saveNewTeamToRepository(TeamDTO teamDTO) {
        return teamMapper.toDTO(teamRepository.save(teamMapper.toEntity(teamDTO)));
    }

    public Pair<TeamDTO, Integer> updateTeamIfNeeded(TeamDTO newTeam) {
        int updatedTeams = 0;
        TeamDTO currentTeam = teamMapper.toDTO(teamRepository.findByUri(newTeam.getUri()));

        if (!currentTeam.equals(newTeam)) {
            updatedTeams += teamRepository.updateTeamFields(currentTeam.getId(), newTeam.getName(), newTeam.getCurrentLeagueId());
        }

        return new Pair<>(currentTeam, updatedTeams);
    }

    public void processNewTeam(TeamTableTeam teamTableTeam, TeamUpdateResult teamUpdateResult) {
        TeamDTO newTeamDTO = saveNewTeamToRepository(teamTableTeam.getTeam());
        int updatedTableTeams = tableTeamProcessor.updateTableTeamIfNeeded(teamTableTeam.getTableTeam(), newTeamDTO);
        teamUpdateResult.incrementTeams(1);
        teamUpdateResult.incrementTableTeams(updatedTableTeams);
    }

    public void processExistingTeam(TeamTableTeam teamTableTeam, TeamUpdateResult teamUpdateResult) {
        Pair<TeamDTO, Integer> idAndUpdatedTeams = updateTeamIfNeeded(teamTableTeam.getTeam());
        int updatedTableTeams = tableTeamProcessor.updateTableTeamIfNeeded(teamTableTeam.getTableTeam(), idAndUpdatedTeams.getFirst());
        teamUpdateResult.incrementTeams(idAndUpdatedTeams.getSecond());
        teamUpdateResult.incrementTableTeams(updatedTableTeams);
    }

    public void enhanceTeamWithTableTeam(TeamDTO team) {
        team.setCurrentTableTeam(tableTeamProcessor.getTableTeamByTeamAndLeague(team.getId(), team.getCurrentLeagueId()));
    }
}
