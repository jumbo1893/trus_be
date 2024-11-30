package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.repository.football.TeamRepository;
import com.jumbo.trus.mapper.football.TeamMapper;
import com.jumbo.trus.service.football.helper.TeamTableTeam;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final Logger logger = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final TeamProcessor teamProcessor;
    private final TeamRetriever teamRetriever;

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toDTO).toList();
    }

    public List<TeamDTO> getAllTeamsFromCurrentSeason() {
        return teamRepository.findTeamsFromCurrentSeason().stream().map(teamMapper::toDTO).toList();
    }

    public TeamDTO getTeamByUri(String uri) {
        return teamRepository.existsByUri(uri)
                ? teamMapper.toDTO(teamRepository.findByUri(uri))
                : null;
    }

    public void updateTeams() {
        logger.debug("updatuji týmy z PKFL");
        List<TeamTableTeam> teamTableTeams = teamRetriever.retrieveTeams(teamRetriever.isUpdateNeeded());
        TeamUpdateResult teamUpdateResult = processTeams(teamTableTeams);
        logger.debug("update týmů dokončen, celkem se prolezlo týmů: {}", teamTableTeams.size());
        logger.debug("počet aktualizovaných týmů: {}", teamUpdateResult.getUpdatedTeams());
        logger.debug("počet aktualizovaných tabulkovaných týmů: {}", teamUpdateResult.getUpdatedTableTeams());
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
