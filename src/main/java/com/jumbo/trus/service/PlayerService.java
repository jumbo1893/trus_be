package com.jumbo.trus.service;

import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerMapper playerMapper;

    public PlayerDTO addPlayer(PlayerDTO playerDTO) {
        PlayerEntity entity = playerMapper.toEntity(playerDTO);
        PlayerEntity savedEntity = playerRepository.save(entity);
        return playerMapper.toDTO(savedEntity);
    }

    public List<PlayerDTO> getAllByFan(boolean fan, int limit){
        List<PlayerEntity> playerEntities = playerRepository.getAllByFan(fan);
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
        System.out.println(entity);
        PlayerEntity savedEntity = playerRepository.save(entity);
        return playerMapper.toDTO(savedEntity);
    }

    public void deletePlayer(Long playerId) {
        playerRepository.deleteById(playerId);
    }

    public PlayerDTO getPlayer(Long playerId) {
        PlayerEntity entity = playerRepository.getReferenceById(playerId);
        return playerMapper.toDTO(entity);
    }
}
