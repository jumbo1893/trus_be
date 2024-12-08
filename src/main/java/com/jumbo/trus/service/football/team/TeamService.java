package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.repository.football.TeamRepository;
import com.jumbo.trus.mapper.football.TeamMapper;
import com.jumbo.trus.service.football.helper.TeamTableTeam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final TeamProcessor teamProcessor;
    private final TeamRetriever teamRetriever;

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toDTO).toList();
    }

    public List<TableTeamDTO> getTable(Long teamId) {
        return teamProcessor.getTableTeamsByTeamId(teamId);
    }


    public List<TeamDTO> getAllTeamsFromCurrentSeason() {
        return teamRepository.findTeamsFromCurrentSeason().stream().map(teamMapper::toDTO).toList();
    }

    public TeamDTO getTeamByUri(String uri) {
        return teamRepository.existsByUri(uri)
                ? teamMapper.toDTO(teamRepository.findByUri(uri))
                : null;
    }

    public void enhanceTeamWithFootballTeam(TeamDTO teamDTO) {
        teamProcessor.enhanceTeamWithTableTeam(teamDTO);
    }

    public void updateTeams() {
        log.debug("updatuji týmy z PKFL");
        List<TeamTableTeam> teamTableTeams = teamRetriever.retrieveTeams(teamRetriever.isUpdateNeeded());
        TeamUpdateResult teamUpdateResult = processTeams(teamTableTeams);
        log.debug("update týmů dokončen, celkem se prolezlo týmů: {}", teamTableTeams.size());
        log.debug("počet aktualizovaných týmů: {}", teamUpdateResult.getUpdatedTeams());
        log.debug("počet aktualizovaných tabulkovaných týmů: {}", teamUpdateResult.getUpdatedTableTeams());
    }

    private TeamUpdateResult processTeams(List<TeamTableTeam> teamTableTeams) {
        TeamUpdateResult teamUpdateResult = new TeamUpdateResult();
        for (TeamTableTeam teamTableTeam : teamTableTeams) {
            if (teamProcessor.isNewTeam(teamTableTeam.getTeam())) {
                teamProcessor.processNewTeam(teamTableTeam, teamUpdateResult);
            } else {
                teamProcessor.processExistingTeam(teamTableTeam, teamUpdateResult);
            }
        }

        return teamUpdateResult;
    }

}
