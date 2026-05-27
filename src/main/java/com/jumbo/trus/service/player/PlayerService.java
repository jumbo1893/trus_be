package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.BeerRepository;
import com.jumbo.trus.repository.GoalRepository;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.helper.BirthdayCalculator;
import com.jumbo.trus.service.helper.ValidationField;
import com.jumbo.trus.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.Date;
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
        validatePlayer(playerDTO);
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
        notificationService.addNotification("Smazán " + (playerEntity.isFan() ? "fanoušek" : "hráč"), playerEntity.getName() + ", s narozeninami " + playerEntity.getBirthday());
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

    public PlayerDTO noPlayer() {
        PlayerDTO playerDTO = new PlayerDTO();
        playerDTO.setId(0L);
        playerDTO.setFootballPlayer(null);
        playerDTO.setName("-");
        playerDTO.setFan(false);
        playerDTO.setBirthday(new Date());
        playerDTO.setActive(true);
        return playerDTO;
    }

    private void validatePlayer(PlayerDTO playerDTO) {
        if (playerDTO.getFootballPlayer() == null) return;
        FootballPlayerEntity newFootballPlayerEntity = footballPlayerService.getFootballPlayerEntity(playerDTO.getFootballPlayer().getId());
        if (newFootballPlayerEntity.getPlayer() == null || newFootballPlayerEntity.getPlayer().getId() == playerDTO.getId()) return;
        makeValidationException(newFootballPlayerEntity.getPlayer().getName(), newFootballPlayerEntity.getName());
    }

    private void makeValidationException(String oldPlayerName, String footballerName) {
        List<ValidationField> fields = new ArrayList<>();
        fields.add(new ValidationField("football_player", "Pod hráčem " + footballerName + " hraje již " +  oldPlayerName));
        throw new FieldValidationException("Chyba při úpravě hráče", fields);
    }
}
