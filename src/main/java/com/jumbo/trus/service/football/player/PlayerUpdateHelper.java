package com.jumbo.trus.service.football.player;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.entity.repository.football.FootballPlayerRepository;
import com.jumbo.trus.mapper.football.FootballPlayerMapper;
import com.jumbo.trus.service.helper.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerUpdateHelper {

    private final FootballPlayerRepository footballPlayerRepository;
    private final FootballPlayerMapper footballPlayerMapper;

    public boolean isNewPlayer(FootballPlayerDTO player) {
        return !footballPlayerRepository.existsByUri(player.getUri());
    }

    public FootballPlayerDTO saveNewPlayer(FootballPlayerDTO footballPlayerDTO) {
        return footballPlayerMapper.toDTO(
            footballPlayerRepository.save(footballPlayerMapper.toEntity(footballPlayerDTO))
        );
    }

    public Pair<Long, Integer> updatePlayerIfNeeded(FootballPlayerDTO newPlayer) {
        FootballPlayerDTO currentPlayer = footballPlayerMapper.toDTO(
            footballPlayerRepository.findByUri(newPlayer.getUri())
        );
        Long playerId = currentPlayer.getId();
        if (!currentPlayer.equals(newPlayer)) {
            int updatedPlayers = footballPlayerRepository.updatePlayerFields(
                playerId, newPlayer.getName(), newPlayer.getBirthYear(),
                newPlayer.getEmail(), newPlayer.getPhoneNumber()
            );
            return new Pair<>(playerId, updatedPlayers);
        }
        return new Pair<>(playerId, 0);
    }

    public Pair<Long, Integer> saveOrUpdateFootballerIfNeeded(FootballPlayerDTO newPlayer) {
        Long playerId;
        if (isNewPlayer(newPlayer)) {
            playerId = saveNewPlayer(newPlayer).getId();
            return new Pair<>(playerId, 1);
        }
        return updatePlayerIfNeeded(newPlayer);
    }
}
