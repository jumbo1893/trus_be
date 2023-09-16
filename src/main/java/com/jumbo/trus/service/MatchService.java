package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.filter.BaseSeasonFilter;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.repository.specification.MatchSpecification;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;
import static com.jumbo.trus.config.Config.AUTOMATIC_SEASON_ID;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MatchMapper matchMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private SeasonService seasonService;

    @Autowired
    private PlayerService playerService;

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

    public List<MatchDTO> getMatchesByDate(int limit){
        return matchRepository.getMatchesOrderByDate(limit).stream().map(matchMapper::toDTO).collect(Collectors.toList());
    }

    public List<PlayerDTO> getPlayerListByMatchId(Long matchId){
        MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
        return matchEntity.getPlayerList().stream().map(playerMapper::toDTO).toList();
    }

    public List<PlayerDTO> getPlayerListByFilteredByFansByMatchId(Long matchId, boolean fan){
        MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
        List<PlayerDTO> playerDTOS = matchEntity.getPlayerList().stream().map(playerMapper::toDTO).toList();
        List<PlayerDTO> players = new ArrayList<>();
        for (PlayerDTO playerDTO : playerDTOS) {
            if (playerDTO.isFan() == fan) {
                players.add(playerDTO);
            }
        }
        return players;
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

    public void updateSeasonId(Long seasonId) {
        matchRepository.updateSeasonId(seasonId);
    }

    @Transactional
    public void deleteMatch(Long matchId) {
        matchRepository.deleteByPlayersInMatchByMatchId(matchId);
        receivedFineRepository.deleteByMatchId(matchId);
        goalRepository.deleteByMatchId(matchId);
        beerRepository.deleteByMatchId(matchId);
        matchRepository.deleteById(matchId);
    }

    public SetupMatchResponse setupMatch(Long matchId) {
        SetupMatchResponse response = new SetupMatchResponse();
        if (matchId != null) {
            MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
            if (matchEntity != null) {
                response.setMatch(matchMapper.toDTO(matchEntity));
                response.setPrimarySeason(seasonService.getSeason(matchEntity.getSeason().getId()));
            }
        }
        else {
            response.setPrimarySeason(seasonService.getAutomaticSeason());
        }
        response.setSeasonList(seasonService.getAll(new SeasonFilter(false, true, true)));
        response.setFanList(playerService.getAllByFan(true));
        response.setPlayerList(playerService.getAllByFan(false));
        return response;
    }

    public MatchDTO getMatch(long matchId) {
        MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
        return matchMapper.toDTO(matchEntity);
    }

    public MatchDTO getLatestMatchBySeasonId(long seasonId) {
        MatchEntity matchEntity;
        if (seasonId == ALL_SEASON_ID) {
            List<MatchEntity> matchEntities = matchRepository.getMatchesOrderByDate(1);
            if (matchEntities.isEmpty()) {
                return null;
            }
            matchEntity = matchEntities.get(0);
        }
        else {
            matchEntity = matchRepository.getLastMatchBySeasonId(seasonId);
        }
        if (matchEntity != null) {
            return matchMapper.toDTO(matchEntity);
        }
        return null;
    }

    public PairSeasonMatch returnSeasonAndMatchByFilter(BaseSeasonFilter filter) {
        MatchDTO matchDTO;
        SeasonDTO seasonDTO;
        if (filter.getMatchId() != null) {
            matchDTO = getMatch(filter.getMatchId());
            if(filter.getSeasonId() != null) {
                seasonDTO = seasonService.getSeason(filter.getSeasonId());
            }
            else {
                seasonDTO = seasonService.getSeason(matchDTO.getSeasonId());
            }

        } else if (filter.getSeasonId() != null) {
            matchDTO = getLatestMatchBySeasonId(filter.getSeasonId());
            seasonDTO = seasonService.getSeason(filter.getSeasonId());
        }
        else {
            seasonDTO = seasonService.getCurrentSeason();
            matchDTO = getLatestMatchBySeasonId(seasonDTO.getId());
        }
        return new PairSeasonMatch(seasonDTO, matchDTO);
    }

    private void mapPlayersAndSeasonToMatch(MatchEntity match, MatchDTO matchDTO){
        match.setPlayerList(new ArrayList<>());
        List<PlayerEntity> people = playerRepository.findAllById(matchDTO.getPlayerIdList());
        match.getPlayerList().addAll(people);
        if(matchDTO.getSeasonId() == AUTOMATIC_SEASON_ID) {
            long newSeasonId = seasonService.getCurrentSeason().getId();
            matchDTO.setSeasonId(newSeasonId);
        }
        match.setSeason(seasonRepository.getReferenceById(matchDTO.getSeasonId()));
    }

}
