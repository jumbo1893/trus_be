package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.goal.multi.GoalListDTO;
import com.jumbo.trus.dto.goal.response.GoalMultiAddResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalSetupResponse;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.entity.GoalEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.filter.GoalFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.entity.repository.specification.GoalSpecification;
import com.jumbo.trus.entity.repository.specification.GoalStatsSpecification;
import com.jumbo.trus.mapper.*;
import com.jumbo.trus.service.order.OrderGoalDetailedDTOByGoalNumber;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoalService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private GoalMapper goalMapper;

    @Autowired
    private GoalDetailedMapper goalDetailedMapper;

    @Autowired
    private GoalSetupMapper goalSetupMapper;

    @Autowired
    private MatchService matchService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ReceivedFineService receivedFineService;

    public GoalDTO addGoal(GoalDTO goalDTO) {
        GoalEntity entity = goalMapper.toEntity(goalDTO);
        mapPlayerAndMatch(entity, goalDTO);
        GoalEntity savedEntity = goalRepository.save(entity);
        return goalMapper.toDTO(savedEntity);
    }

    @Transactional
    public GoalMultiAddResponse addMultipleGoal(GoalListDTO goalListDTO) {
        GoalMultiAddResponse goalMultiAddResponse = new GoalMultiAddResponse();
        goalMultiAddResponse.setMatch(matchRepository.getReferenceById(goalListDTO.getMatchId()).getName());
        for (GoalDTO goalDTO : goalListDTO.getGoalList()) {
            goalDTO.setMatchId(goalListDTO.getMatchId());
            GoalDTO oldGoal = getGoalDtoByPlayerAndMatch(goalListDTO.getMatchId(), goalDTO.getPlayerId());
            int goalDiff = checkGoalDiff(oldGoal, goalDTO);
            int assistDiff = checkAssistDiff(oldGoal, goalDTO);
            if (oldGoal != null && (goalDiff != 0 || assistDiff != 0)) {
                goalMultiAddResponse.addGoals(goalDiff);
                goalMultiAddResponse.addAssists(assistDiff);
                goalDTO.setId(oldGoal.getId());
                goalMultiAddResponse.setMatch(saveGoalToRepository(goalDTO).getMatch().getName());
            } else if (isNeededToAddGoal(oldGoal, goalDTO)) {
                goalMultiAddResponse.addGoals(goalDTO.getGoalNumber());
                goalMultiAddResponse.addAssists(goalDTO.getAssistNumber());
                goalMultiAddResponse.setMatch(saveGoalToRepository(goalDTO).getMatch().getName());
            }
        }
        if (goalListDTO.isRewriteToFines()) {
            rewriteFinesInDB(goalListDTO.getMatchId(), goalListDTO.getGoalList());
        }
        return goalMultiAddResponse;
    }

    private boolean isNeededToAddGoal(GoalDTO oldGoal, GoalDTO goalDTO) {
        return oldGoal == null && (goalDTO.getGoalNumber() != 0 || goalDTO.getAssistNumber() != 0);
    }

    private int checkGoalDiff(GoalDTO oldGoal, GoalDTO goalDTO) {
        if (oldGoal == null) {
            return 0;
        }
        return goalDTO.getGoalNumber()-oldGoal.getGoalNumber();
    }

    private int checkAssistDiff(GoalDTO oldGoal, GoalDTO goalDTO) {
        if (oldGoal == null) {
            return 0;
        }
        return goalDTO.getAssistNumber()-oldGoal.getAssistNumber();
    }

    public List<GoalDTO> getAll(GoalFilter goalFilter) {
        GoalSpecification goalSpecification = new GoalSpecification(goalFilter);
        return goalRepository.findAll(goalSpecification, PageRequest.of(0, goalFilter.getLimit())).stream().map(goalMapper::toDTO).collect(Collectors.toList());
    }

    public GoalDetailedResponse getAllDetailed(StatisticsFilter filter) {
        GoalDetailedResponse goalDetailedResponse = new GoalDetailedResponse();
        GoalStatsSpecification goalSpecification = new GoalStatsSpecification(filter);
        List<GoalDetailedDTO> goalList = goalRepository.findAll(goalSpecification, PageRequest.of(0, filter.getLimit())).stream().map(goalDetailedMapper::toDTO).toList();
        Set<Long> matchSet = new HashSet<>();
        Set<Long> playerSet = new HashSet<>();
        HashMap<Long, GoalDetailedDTO> matchMap = new HashMap<>();
        HashMap<Long, GoalDetailedDTO> playerMap = new HashMap<>();
        for (GoalDetailedDTO goal : goalList) {
            goalDetailedResponse.addGoals(goal.getGoalNumber());
            goalDetailedResponse.addAssists(goal.getAssistNumber());
            matchSet.add(goal.getMatch().getId());
            playerSet.add(goal.getPlayer().getId());
            if (filter.getMatchStatsOrPlayerStats() != null && !filter.getMatchStatsOrPlayerStats()) {
                goal.setMatch(null);
                if (!playerMap.containsKey(goal.getPlayer().getId())) {
                    playerMap.put(goal.getPlayer().getId(), goal);
                }
                else {
                    GoalDetailedDTO oldGoal = playerMap.get(goal.getPlayer().getId());
                    oldGoal.addGoals(goal.getGoalNumber());
                    oldGoal.addAssists(goal.getAssistNumber());
                    playerMap.put(goal.getPlayer().getId(), oldGoal);
                }
            }
            if (filter.getMatchStatsOrPlayerStats() != null && filter.getMatchStatsOrPlayerStats()) {
                goal.setPlayer(null);
                if (!matchMap.containsKey(goal.getMatch().getId())) {
                    matchMap.put(goal.getMatch().getId(), goal);
                }
                else {
                    GoalDetailedDTO oldGoal = matchMap.get(goal.getMatch().getId());
                    oldGoal.addGoals(goal.getGoalNumber());
                    oldGoal.addAssists(goal.getAssistNumber());
                    matchMap.put(goal.getMatch().getId(), oldGoal);
                }
            }
        }
        List<GoalDetailedDTO> returnGoalList;
        if (filter.getMatchStatsOrPlayerStats() != null && filter.getMatchStatsOrPlayerStats()) {
            returnGoalList = new ArrayList<>(matchMap.values().stream().toList());
        }
        else if (filter.getMatchStatsOrPlayerStats() != null) {
            returnGoalList = new ArrayList<>(playerMap.values().stream().toList());
        }
        else {
            returnGoalList = new ArrayList<>(goalList);
        }
        returnGoalList.sort(new OrderGoalDetailedDTOByGoalNumber());
        goalDetailedResponse.setGoalList(returnGoalList);
        goalDetailedResponse.setMatchesCount(matchSet.size());
        goalDetailedResponse.setPlayersCount(playerSet.size());
        return goalDetailedResponse;
    }

    public List<GoalSetupResponse> getGoalSetup(GoalFilter goalFilter) {
        GoalSpecification goalSpecification = new GoalSpecification(goalFilter);
        if (goalFilter.getMatchId() == null) {
            throw new NotFoundException("Není vyplněné ID");
        }
        List<PlayerDTO> playersFromMatch = matchService.getPlayerListByMatchId(goalFilter.getMatchId());
        List<GoalSetupResponse> goalList = goalRepository.findAll(goalSpecification, PageRequest.of(0, goalFilter.getLimit())).stream().map(goalSetupMapper::toDTO).toList();
        return setPlayersForGoalList(playersFromMatch, goalList);
    }

    public void deleteGoal(Long goalId) {
        goalRepository.deleteById(goalId);
    }

    private void mapPlayerAndMatch(GoalEntity goal, GoalDTO goalDTO) {
        goal.setMatch(matchRepository.getReferenceById(goalDTO.getMatchId()));
        goal.setPlayer(playerRepository.getReferenceById(goalDTO.getPlayerId()));
    }

    private GoalEntity saveGoalToRepository(GoalDTO goalDTO) {
        GoalEntity entity = goalMapper.toEntity(goalDTO);
        mapPlayerAndMatch(entity, goalDTO);
        return goalRepository.save(entity);
    }

    @Transactional
    private void rewriteFinesInDB(Long matchId, List<GoalDTO> goalList) {
        receivedFineRepository.deleteGoalAndHattrickFinesFromMatch(matchId, Config.GOAL_FINE_ID, Config.HATTRICK_FINE_ID);
        for (GoalDTO goalDTO : goalList) {
            if (goalDTO.getGoalNumber() > 0) {
                addGoalFine(goalDTO.getGoalNumber(), goalDTO.getPlayerId(), matchId);
                if (goalDTO.getGoalNumber() > 2) {
                    setAndAddHattrickFines(matchId, goalDTO.getGoalNumber()/3, goalDTO.getPlayerId());
                }
            }
        }

    }

    private void addGoalFine(int number, long playerId, long matchId) {
        FineDTO goalFine = new FineDTO(Config.GOAL_FINE_ID, "", 0);
        ReceivedFineDTO receivedFine = new ReceivedFineDTO(number, goalFine, playerId, matchId);
        receivedFineService.addFine(receivedFine);
    }

    private void setAndAddHattrickFines(long matchId, int numberOfHattricks, long hattrickPlayerId) {
        List<Long> playerIds = playerService.convertPlayerListToPlayerIdList(playerService.getAllActive(true));
        for (Long playerId : playerIds) {
            if (playerId != hattrickPlayerId) {
                addHattrickFine(numberOfHattricks, playerId, matchId);
            }
        }
    }

    private void addHattrickFine(int number, long playerId, long matchId) {
        FineDTO hattrickFine = new FineDTO(Config.HATTRICK_FINE_ID, "", 0);
        ReceivedFineDTO receivedFine = new ReceivedFineDTO(number, hattrickFine, playerId, matchId);
        receivedFineService.addFine(receivedFine);
    }


    /**
     * @param matchId  id zápasu
     * @param playerId id hráče
     * @return null pokud neexistuje
     */
    private GoalDTO getGoalDtoByPlayerAndMatch(Long matchId, Long playerId) {
        GoalFilter goalFilter = new GoalFilter(matchId, playerId);
        GoalSpecification goalSpecification = new GoalSpecification(goalFilter);
        List<GoalDTO> filterList = goalRepository.findAll(goalSpecification, PageRequest.of(0, 1)).stream().map(goalMapper::toDTO).toList();
        if (filterList.isEmpty()) {
            return null;
        }
        return filterList.get(0);
    }

    private List<GoalSetupResponse> setPlayersForGoalList(List<PlayerDTO> playersFromMatch, List<GoalSetupResponse> goalListFromDB) {
        List<GoalSetupResponse> goalList = new ArrayList<>();
        Set<Long> playerIdsGoal = new HashSet<>();
        for (GoalSetupResponse goal : goalListFromDB) {
            playerIdsGoal.add(goal.getPlayer().getId());
        }
        for (PlayerDTO player : playersFromMatch) {
            if (!playerIdsGoal.contains(player.getId()) && !player.isFan()) {
                goalList.add(new GoalSetupResponse().newGoalSetup(player));
            }
        }
        for (GoalSetupResponse goal : goalListFromDB) {
            goalList.add(0, goal);
        }
        return goalList;
    }
}
