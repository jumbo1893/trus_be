package com.jumbo.trus.service.football.player;

import com.jumbo.trus.repository.football.FootballPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamPlayerSyncHelper {

    private final FootballPlayerRepository footballPlayerRepository;

    public int synchronizeTeamPlayers(List<Long> currentPlayerIds, List<Long> newPlayerIds, Long teamId) {
        int removedPlayers = handleMissingPlayers(currentPlayerIds, newPlayerIds, teamId);
        int addedPlayers = handleExtraPlayers(currentPlayerIds, newPlayerIds, teamId);
        return removedPlayers + addedPlayers;
    }

    private int handleMissingPlayers(List<Long> currentPlayerIds, List<Long> newPlayerIds, Long teamId) {
        List<Long> missingInNewPlayers = new ArrayList<>(currentPlayerIds);
        missingInNewPlayers.removeAll(newPlayerIds);
        missingInNewPlayers.forEach(playerId -> footballPlayerRepository.deleteTeamPlayers(teamId, playerId));
        return missingInNewPlayers.size();
    }

    private int handleExtraPlayers(List<Long> currentPlayerIds, List<Long> newPlayerIds, Long teamId) {
        List<Long> extraInNewPlayers = new ArrayList<>(newPlayerIds);
        extraInNewPlayers.removeAll(currentPlayerIds);
        extraInNewPlayers.forEach(playerId -> footballPlayerRepository.saveNewTeamPlayer(teamId, playerId));
        return extraInNewPlayers.size();
    }
}
