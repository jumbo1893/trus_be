package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.repository.football.TableTeamRepository;
import com.jumbo.trus.mapper.football.TableTeamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TableTeamProcessor {

    private final TableTeamRepository tableTeamRepository;
    private final TableTeamMapper tableTeamMapper;

    public int updateTableTeamIfNeeded(TableTeamDTO newTableTeam, TeamDTO teamDTO) {
        TableTeamDTO currentTableTeam = getTableTeamByTeamAndLeague(teamDTO.getId(), newTableTeam.getLeague().getId());
        if (currentTableTeam == null) {
            newTableTeam.setTeam(teamDTO);
            tableTeamRepository.save(tableTeamMapper.toEntity(newTableTeam));
            return 1;
        }

        if (!currentTableTeam.equals(newTableTeam)) {
            newTableTeam.setId(currentTableTeam.getId());
            newTableTeam.setTeam(teamDTO);
            tableTeamRepository.save(tableTeamMapper.toEntity(newTableTeam));
            return 1;
        }

        return 0;
    }

    public List<TableTeamDTO> getTableTeamsByTeam(TeamDTO teamDTO) {
        return tableTeamRepository.findByLeagueIdOrderByPointsDesc(teamDTO.getCurrentLeagueId()).stream().map(tableTeamMapper::toDTO).toList();
    }

    public TableTeamDTO getTableTeamByTeamAndLeague(Long teamId, Long leagueId) {
        return tableTeamMapper.toDTO(tableTeamRepository.findByTeamIdAndLeagueId(teamId, leagueId));
    }
}
