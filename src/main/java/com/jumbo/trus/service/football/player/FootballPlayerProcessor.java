package com.jumbo.trus.service.football.player;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.repository.football.FootballPlayerRepository;
import com.jumbo.trus.service.football.pkfl.task.RetrieveTeamPlayers;
import com.jumbo.trus.service.helper.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FootballPlayerProcessor {
    private final FootballPlayerRepository footballPlayerRepository;
    private final RetrieveTeamPlayers retrieveTeamPlayers;
    private final PlayerUpdateHelper playerUpdateHelper;
    private final TeamPlayerSyncHelper teamPlayerSyncHelper;

    public int processPlayer(TeamDTO team) {
        int updatedFootballers = 0;
        List<FootballPlayerDTO> footballers = retrieveTeamPlayers.getFootballers(team);
        List<Long> footballPlayerIds = new ArrayList<>();
        for (FootballPlayerDTO footballPlayerDTO : footballers) {
            Pair<Long, Integer> idAndUpdatedFootballers = playerUpdateHelper.saveOrUpdateFootballerIfNeeded(footballPlayerDTO);
            updatedFootballers += idAndUpdatedFootballers.getSecond();
            footballPlayerIds.add(idAndUpdatedFootballers.getFirst());
        }
        updatedFootballers += teamPlayerSyncHelper.synchronizeTeamPlayers(returnPlayerIdsByTeam(team.getId()), footballPlayerIds, team.getId());
        return updatedFootballers;
    }

    private List<Long> returnPlayerIdsByTeam(Long teamId) {
        return footballPlayerRepository.findAllPlayersIdsByTeamId(teamId);
    }
}
