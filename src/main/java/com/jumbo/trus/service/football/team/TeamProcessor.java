package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.repository.football.TableTeamRepository;
import com.jumbo.trus.entity.repository.football.TeamRepository;
import com.jumbo.trus.mapper.football.TableTeamMapper;
import com.jumbo.trus.mapper.football.TeamMapper;
import com.jumbo.trus.service.football.helper.TeamTableTeam;
import com.jumbo.trus.service.helper.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamProcessor {

    private final TeamRepository teamRepository;
    private final TableTeamRepository tableTeamRepository;
    private final TeamMapper teamMapper;
    private final TableTeamMapper tableTeamMapper;

    public boolean isNewTeam(TeamDTO team) {
        return !teamRepository.existsByUri(team.getUri());
    }

    public Long saveNewTeamToRepository(TeamDTO teamDTO) {
        return teamRepository.save(teamMapper.toEntity(teamDTO)).getId();
    }

    public Pair<Long, Integer> updateTeamIfNeeded(TeamDTO newTeam) {
        int updatedTeams = 0;
        TeamDTO currentTeam = teamMapper.toDTO(teamRepository.findByUri(newTeam.getUri()));
        Long currentTeamId = currentTeam.getId();

        if (!currentTeam.equals(newTeam)) {
            updatedTeams += teamRepository.updateTeamFields(currentTeamId, newTeam.getName(), newTeam.getCurrentLeagueId());
        }

        return new Pair<>(currentTeamId, updatedTeams);
    }

    public int updateTableTeamIfNeeded(TableTeamDTO newTableTeam, Long teamId) {
        TableTeamDTO currentTableTeam = tableTeamMapper.toDTO(
            tableTeamRepository.findByTeamIdAndLeagueId(teamId, newTableTeam.getLeagueId())
        );

        if (currentTableTeam == null) {
            newTableTeam.setTeamId(teamId);
            tableTeamRepository.save(tableTeamMapper.toEntity(newTableTeam));
            return 1;
        }

        if (!currentTableTeam.equals(newTableTeam)) {
            newTableTeam.setId(currentTableTeam.getId());
            newTableTeam.setTeamId(teamId);
            tableTeamRepository.save(tableTeamMapper.toEntity(newTableTeam));
            return 1;
        }

        return 0;
    }

    public void processNewTeam(TeamTableTeam teamTableTeam, TeamUpdateResult teamUpdateResult) {
        Long newTeamId = saveNewTeamToRepository(teamTableTeam.getTeam());
        int updatedTableTeams = updateTableTeamIfNeeded(teamTableTeam.getTableTeam(), newTeamId);
        teamUpdateResult.incrementTeams(1);
        teamUpdateResult.incrementTableTeams(updatedTableTeams);
    }

    public void processExistingTeam(TeamTableTeam teamTableTeam, TeamUpdateResult teamUpdateResult) {
        Pair<Long, Integer> idAndUpdatedTeams = updateTeamIfNeeded(teamTableTeam.getTeam());
        int updatedTableTeams = updateTableTeamIfNeeded(teamTableTeam.getTableTeam(), idAndUpdatedTeams.getFirst());
        teamUpdateResult.incrementTeams(idAndUpdatedTeams.getSecond());
        teamUpdateResult.incrementTableTeams(updatedTableTeams);
    }
}
