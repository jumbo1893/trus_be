package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.football.TableTeamEntity;
import com.jumbo.trus.entity.repository.football.TableTeamRepository;
import com.jumbo.trus.mapper.football.TableTeamMapper;
import com.jumbo.trus.mapper.football.TeamMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TableTeamProcessor {

    private final TableTeamRepository tableTeamRepository;
    private final TableTeamMapper tableTeamMapper;
    private final TeamMapper teamMapper;

    public int updateTableTeamIfNeeded(TableTeamDTO newTableTeam, TeamDTO teamDTO) {
        TableTeamDTO currentTableTeam = getTableTeamByTeamAndLeague(teamDTO.getId(), newTableTeam.getLeague().getId());
        if (currentTableTeam == null) {
            saveTableTeam(newTableTeam, teamDTO);
            return 1;
        }

        if (!currentTableTeam.equals(newTableTeam)) {
            newTableTeam.setId(currentTableTeam.getId());
            saveTableTeam(newTableTeam, teamDTO);
            return 1;
        }

        return 0;
    }

    private void saveTableTeam(TableTeamDTO tableTeamDTO, TeamDTO teamDTO) {
        TableTeamEntity tableTeam = tableTeamMapper.toEntity(tableTeamDTO);
        tableTeam.setTeam(teamMapper.toEntity(teamDTO));
        tableTeamRepository.save(tableTeam);
    }

    public List<TableTeamDTO> getTableTeamsByTeam(TeamDTO teamDTO) {
        return tableTeamRepository.findByLeagueIdOrderByPointsDesc(teamDTO.getCurrentLeagueId()).stream().map(tableTeamMapper::toDTO).toList();
    }

    public TableTeamDTO getTableTeamByTeamAndLeague(Long teamId, Long leagueId) {
        return tableTeamMapper.toDTO(tableTeamRepository.findByTeamIdAndLeagueId(teamId, leagueId));
    }

    public TableTeamDTO getTableTeamById(long tableTeamId) {
        return tableTeamMapper.toDTO(tableTeamRepository.findById(tableTeamId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(tableTeamId))));
    }
}
