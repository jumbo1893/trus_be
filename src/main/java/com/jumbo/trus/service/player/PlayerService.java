package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.player.PlayerSetup;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.entity.repository.GoalRepository;
import com.jumbo.trus.entity.repository.PlayerRepository;
import com.jumbo.trus.entity.repository.ReceivedFineRepository;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.service.NotificationService;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.helper.BirthdayCalculator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final BeerRepository beerRepository;
    private final ReceivedFineRepository receivedFineRepository;
    private final GoalRepository goalRepository;
    private final NotificationService notificationService;
    private final FootballPlayerService footballPlayerService;
    private final FootballPlayerStatsService footballPlayerStatsService;

    public PlayerDTO addPlayer(PlayerDTO playerDTO, AppTeamEntity appTeam) {
        PlayerEntity entity = playerMapper.toEntity(playerDTO);
        entity.setAppTeam(appTeam);
        PlayerEntity savedEntity = playerRepository.save(entity);
        notificationService.addNotification("Přidán " + (playerDTO.isFan() ? "fanoušek" : "hráč"), playerDTO.getName() + ", s narozeninami " + playerDTO.getBirthday());
        return playerMapper.toDTO(savedEntity);
    }

    public List<PlayerDTO> getAllByFan(boolean fan, long appTeamId){
        List<PlayerEntity> playerEntities = playerRepository.getAllByFan(fan, appTeamId);
        return playerEntities.stream().map(playerMapper::toDTO).toList();
    }

    public List<PlayerDTO> getAllActive(boolean active, long appTeamId){
        List<PlayerEntity> playerEntities = playerRepository.getAllByActive(active, appTeamId);
        return playerEntities.stream().map(playerMapper::toDTO).toList();
    }

    public List<PlayerDTO> getAll(long appTeamId){
        List<PlayerEntity> playerEntities = playerRepository.getAll(appTeamId);
        return playerEntities.stream().map(playerMapper::toDTO).toList();
    }

    public PlayerDTO getPlayer(long playerId) {
        return playerMapper.toDTO(getPlayerEntity(playerId));
    }

    public PlayerEntity getPlayerEntity(long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(playerId)));
    }

    public String getListOfNamesFromListOfPlayers(List<PlayerDTO> playerList) {
        StringBuilder players = new StringBuilder();
        for (int i = 0; i < playerList.size(); i++) {
            players.append(playerList.get(i).getName());
            if (i != playerList.size()-1) {
                players.append(", ");
            }
        }
        return players.toString();
    }

    public PlayerDTO editPlayer(Long playerId, PlayerDTO playerDTO) throws NotFoundException {
        PlayerEntity foundPlayerEntity = playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Hráč s id " + playerId + "nenalezen v db"));
        PlayerEntity entity = playerMapper.toEntity(playerDTO);
        entity.setId(playerId);
        entity.setAppTeam(foundPlayerEntity.getAppTeam());
        PlayerEntity savedEntity = playerRepository.save(entity);
        notificationService.addNotification("Upraven " + (playerDTO.isFan() ? "fanoušek" : "hráč"), playerDTO.getName() + ", s narozeninami " + playerDTO.getBirthday());
        return playerMapper.toDTO(savedEntity);
    }

    @Transactional
    public void deletePlayer(Long playerId) {
        playerRepository.deleteByPlayersInMatchByPlayerId(playerId);
        receivedFineRepository.deleteByPlayerId(playerId);
        goalRepository.deleteByPlayerId(playerId);
        beerRepository.deleteByPlayerId(playerId);
        PlayerEntity playerEntity = playerRepository.getReferenceById(playerId);
        notificationService.addNotification("Upraven " + (playerEntity.isFan() ? "fanoušek" : "hráč"), playerEntity.getName() + ", s narozeninami " + playerEntity.getBirthday());
        playerRepository.deleteById(playerId);
    }

    public List<Long> convertPlayerListToPlayerIdList(List<PlayerDTO> players) {
        List<Long> playerIdList = new ArrayList<>();
        for (PlayerDTO playerDTO : players) {
            playerIdList.add(playerDTO.getId());
        }
        return playerIdList;
    }

    public String returnNextPlayerBirthdayFromList(long appTeamId) {
        List<PlayerDTO> players = playerRepository.getBirthdayPlayers(appTeamId).stream().map(playerMapper::toDTO).toList();
        BirthdayCalculator birthdayCalculator = new BirthdayCalculator(players);
        return birthdayCalculator.returnNextPlayerBirthdayFromList();
    }

    public PlayerSetup setupPlayer(Long playerId, AppTeamEntity appTeam) {
        PlayerSetup playerSetup = new PlayerSetup();
        List<FootballPlayerDTO> playerList = new ArrayList<>(footballPlayerService.getAllPastPlayersByCurrentTeam(appTeam));
        playerList.add(0, noPlayer());
        playerSetup.setFootballPlayerList(playerList);
        if (playerId != null) {
            playerSetup.setPlayer(getPlayer(playerId));
            FootballPlayerDTO footballPlayerDTO = playerSetup.getPlayer().getFootballPlayer();
            if (footballPlayerDTO != null) {
                playerSetup.setPlayerStats(footballPlayerStatsService.getPlayerStatsForPlayer(playerSetup.getPlayer().getFootballPlayer().getId(), appTeam));
                playerSetup.setPrimaryFootballPlayer(footballPlayerDTO);
            }
            else {
                playerSetup.setPrimaryFootballPlayer(noPlayer());
            }
        }
        else {
            playerSetup.setPrimaryFootballPlayer(noPlayer());
        }
        return playerSetup;
    }

    private FootballPlayerDTO noPlayer() {
        FootballPlayerDTO footballPlayerDTO = new FootballPlayerDTO();
        footballPlayerDTO.setId(0L);
        footballPlayerDTO.setName("-");
        footballPlayerDTO.setBirthYear(0);
        footballPlayerDTO.setUri("");
        return footballPlayerDTO;
    }
}
