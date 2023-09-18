package com.jumbo.trus.service;

import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.entity.repository.GoalRepository;
import com.jumbo.trus.entity.repository.ReceivedFineRepository;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.repository.PlayerRepository;
import com.jumbo.trus.service.helper.BirthdayCalculator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private NotificationService notificationService;

    public PlayerDTO addPlayer(PlayerDTO playerDTO) {
        PlayerEntity entity = playerMapper.toEntity(playerDTO);
        PlayerEntity savedEntity = playerRepository.save(entity);
        notificationService.addNotification("Přidán " + (playerDTO.isFan() ? "fanoušek" : "hráč"), playerDTO.getName() + ", s narozeninami " + playerDTO.getBirthday());
        return playerMapper.toDTO(savedEntity);
    }

    public List<PlayerDTO> getAllByFan(boolean fan){
        List<PlayerEntity> playerEntities = playerRepository.getAllByFan(fan);
        List<PlayerDTO> result = new ArrayList<>();
        for(PlayerEntity e : playerEntities){
            result.add(playerMapper.toDTO(e));
        }
        return result;
    }

    public List<PlayerDTO> getAllActive(boolean active){
        List<PlayerEntity> playerEntities = playerRepository.getAllByActive(active);
        List<PlayerDTO> result = new ArrayList<>();
        for(PlayerEntity e : playerEntities){
            result.add(playerMapper.toDTO(e));
        }
        return result;
    }

    public List<PlayerDTO> getAll(int limit){
        List<PlayerEntity> playerEntities = playerRepository.getAll(limit);
        List<PlayerDTO> result = new ArrayList<>();
        for(PlayerEntity e : playerEntities){
            result.add(playerMapper.toDTO(e));
        }
        return result;
    }

    public PlayerDTO editPlayer(Long playerId, PlayerDTO playerDTO) throws NotFoundException {
        if (!playerRepository.existsById(playerId)) {
            throw new NotFoundException("Hráč s id " + playerId + "nenalezen v db");
        }
        PlayerEntity entity = playerMapper.toEntity(playerDTO);
        entity.setId(playerId);
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



    public PlayerDTO getPlayer(Long playerId) {
        PlayerEntity entity = playerRepository.getReferenceById(playerId);
        return playerMapper.toDTO(entity);
    }

    public List<Long> convertPlayerListToPlayerIdList(List<PlayerDTO> players) {
        List<Long> playerIdList = new ArrayList<>();
        for (PlayerDTO playerDTO : players) {
            playerIdList.add(playerDTO.getId());
        }
        return playerIdList;
    }

    public String returnNextPlayerBirthdayFromList() {
        List<PlayerDTO> players = playerRepository.getBirthdayPlayers().stream().map(playerMapper::toDTO).toList();
        BirthdayCalculator birthdayCalculator = new BirthdayCalculator(players);
        return birthdayCalculator.returnNextPlayerBirthdayFromList();
    }
}
