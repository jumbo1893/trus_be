package com.jumbo.trus.service;

import com.jumbo.trus.dto.MatchDTO;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.repository.MatchRepository;
import com.jumbo.trus.entity.repository.PlayerRepository;
import com.jumbo.trus.entity.repository.SeasonRepository;
import com.jumbo.trus.entity.repository.specification.MatchSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private MatchMapper matchMapper;

    public MatchDTO addMatch(MatchDTO matchDTO) {
        MatchEntity entity = matchMapper.toEntity(matchDTO);
        entity.setPlayerList(new ArrayList<>());
        mapPlayersAndSeasonToMatch(entity, matchDTO);
        MatchEntity savedEntity = matchRepository.save(entity);
        return matchMapper.toDTO(savedEntity);
    }

    public List<MatchDTO> getAll(MatchFilter matchFilter){
        MatchSpecification matchSpecification = new MatchSpecification(matchFilter);
        return matchRepository.findAll(matchSpecification, PageRequest.of(0, matchFilter.getLimit())).stream().map(matchMapper::toDTO).collect(Collectors.toList());
    }

    public MatchDTO editMatch(Long matchId, MatchDTO matchDTO) throws NotFoundException {
        if (!matchRepository.existsById(matchId)) {
            throw new NotFoundException("Zápas s id " + matchId + " nenalezen v db");
        }
        MatchEntity entity = matchMapper.toEntity(matchDTO);
        entity.setId(matchId);
        mapPlayersAndSeasonToMatch(entity, matchDTO);
        MatchEntity savedEntity = matchRepository.save(entity);
        return matchMapper.toDTO(savedEntity);
    }

    public void deleteMatch(Long matchId) {
        matchRepository.deleteById(matchId);
    }

    private void mapPlayersAndSeasonToMatch(MatchEntity match, MatchDTO matchDTO){
        match.setPlayerList(new ArrayList<>());
        List<PlayerEntity> people = playerRepository.findAllById(matchDTO.getPlayerIdList());
        match.getPlayerList().addAll(people);
        match.setSeason(seasonRepository.getReferenceById(matchDTO.getSeasonId()));
    }
}
