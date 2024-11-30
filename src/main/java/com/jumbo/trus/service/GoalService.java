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
import com.jumbo.trus.entity.filter.GoalFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.entity.repository.specification.GoalSpecification;
import com.jumbo.trus.entity.repository.specification.GoalStatsSpecification;
import com.jumbo.trus.mapper.*;
import com.jumbo.trus.service.order.OrderGoalDetailedDTOByGoalNumber;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private MatchRepository matchRepository;
    private PlayerRepository playerRepository;
    private ReceivedFineRepository receivedFineRepository;
    private GoalRepository goalRepository;
    private GoalMapper goalMapper;
    private GoalDetailedMapper goalDetailedMapper;
    private GoalSetupMapper goalSetupMapper;
    private MatchService matchService;
    private PlayerService playerService;
    private ReceivedFineService receivedFineService;
    private NotificationService notificationService;

    /**
     * metoda napamuje hráče a zápas z přepravky ke gólu a uloží ho do DB
     * @param goalDTO Gól, který přijde z FE
     * @return Gól z DB
     */
    public GoalDTO addGoal(GoalDTO goalDTO) {
        GoalEntity entity = goalMapper.toEntity(goalDTO);
        mapPlayerAndMatch(entity, goalDTO);
        GoalEntity savedEntity = goalRepository.save(entity);
        return goalMapper.toDTO(savedEntity);
    }

    /**
     * Projde seznam gólů u hráčů a v případě změny zapíše změny do db. Počet změn následně vypíše
     * @param goalListDTO List ve formě přepravky GoalListDTO, který přijde z FE. Obsahuje jak změněné počty gólů u hráčů u konkrétního zápasu, tak může obsahovat i nezměněné počty
     * @return GoalMultiAddResponse - vypsaný počet změn v DB
     */
    @Transactional
    public GoalMultiAddResponse addMultipleGoal(GoalListDTO goalListDTO) {
        StringBuilder newGoalNotification = new StringBuilder();
        StringBuilder newAssistNotification = new StringBuilder();
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
                String playerName = playerRepository.getReferenceById(goalDTO.getPlayerId()).getName();
                if (goalDiff != 0) {
                    newGoalNotification.append(playerName).append(" vstřelil gólů: ").append(goalDTO.getGoalNumber()).append("\n");
                }
                if (assistDiff != 0) {
                    newAssistNotification.append(playerName).append(" má přihrávek: ").append(goalDTO.getAssistNumber()).append("\n");
                }
            } else if (isNeededToAddGoal(oldGoal, goalDTO)) {
                goalMultiAddResponse.addGoals(goalDTO.getGoalNumber());
                goalMultiAddResponse.addAssists(goalDTO.getAssistNumber());
                goalMultiAddResponse.setMatch(saveGoalToRepository(goalDTO).getMatch().getName());
                String playerName = playerRepository.getReferenceById(goalDTO.getPlayerId()).getName();
                if (goalDTO.getGoalNumber() != 0) {
                    newGoalNotification.append(playerName).append(" vstřelil gólů: ").append(goalDTO.getGoalNumber()).append("\n");
                }
                if (goalDTO.getAssistNumber() != 0) {
                    newAssistNotification.append(playerName).append(" má přihrávek: ").append(goalDTO.getAssistNumber()).append("\n");
                }
            }
        }
        if (goalListDTO.isRewriteToFines()) {
            rewriteFinesInDB(goalListDTO.getMatchId(), goalListDTO.getGoalList());
        }
        notificationService.addNotification("Přidány góly/asistence v zápase " + goalMultiAddResponse.getMatch(), newGoalNotification+newAssistNotification.toString());
        return goalMultiAddResponse;
    }

    /**
     *
     * @param oldGoal gol z DB
     * @param goalDTO gól z FE
     * @return Vrátí zda je nutné přidat gól do db (je nesmysl tam dávat že někdo dal 0 gólů to je automatika)
     */
    private boolean isNeededToAddGoal(GoalDTO oldGoal, GoalDTO goalDTO) {
        return oldGoal == null && (goalDTO.getGoalNumber() != 0 || goalDTO.getAssistNumber() != 0);
    }
    /**
     *
     * @param oldGoal gol z DB
     * @param goalDTO gól z FE
     * @return Vrátí rozdíl u gólu
     */
    private int checkGoalDiff(GoalDTO oldGoal, GoalDTO goalDTO) {
        if (oldGoal == null) {
            return 0;
        }
        return goalDTO.getGoalNumber()-oldGoal.getGoalNumber();
    }

    /**
     *
     * @param oldGoal asistence z DB
     * @param goalDTO asistence z FE
     * @return Vrátí rozdíl u asistence
     */
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
    /**
     * metoda prohledá záznamy v DB
     * @param filter filter, podle kterého se vrací počet záznamů. Pomocí parametru matchStatsOrPlayerStats se určuje, zda chceme statistiky z pohledu zápasu (true) či hráče (false)
     *               detailed se nepoužívá
     * @return Vrací rozšířený seznam vstřelenách gólů z db dle filtru
     */
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

    /**
     * Metoda vrátí setup gólů, který se použije v objektu. Jedná se o počet vstřelených gólů a asistencídaném zápase pro dané hráče dle filtru
     * @param goalFilter filter, podle kterého se vrací počet záznamů
     * @return List<GoalSetupResponse>, kde lze najít počet gólů a asistencí pro zápas a jednotlivé hráče
     */
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

    /**
     * metoda vezme góly a dle toho přidá pokuty za góly a hattricky hráčům
     * @param matchId id zápasu
     * @param goalList seznam gólů
     */
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

    /**
     * metoda přidá pokutu za gól
     * @param number počet gólů
     * @param playerId id hráče
     * @param matchId id zápasu
     */
    private void addGoalFine(int number, long playerId, long matchId) {
        FineDTO goalFine = new FineDTO(Config.GOAL_FINE_ID, "", 0, false);
        ReceivedFineDTO receivedFine = new ReceivedFineDTO(number, goalFine, playerId, matchId);
        receivedFineService.addFine(receivedFine);
    }

    /**
     * metoda vyhodnotí, kteří hráči mají dostat pokutu (ostatní, než ten, co dal hattrick) a provolá metzodu na přidání pokut
     * @param numberOfHattricks počet hattricků
     * @param hattrickPlayerId id hráče, který dal hattrick
     * @param matchId id zápasu
     */
    private void setAndAddHattrickFines(long matchId, int numberOfHattricks, long hattrickPlayerId) {
        List<Long> playerIds = playerService.convertPlayerListToPlayerIdList(playerService.getAllActive(true));
        for (Long playerId : playerIds) {
            if (playerId != hattrickPlayerId) {
                addHattrickFine(numberOfHattricks, playerId, matchId);
            }
        }
    }
    /**
     * metoda přidá pokutu za hattrick
     * @param number počet hattricků
     * @param playerId id hráče,
     * @param matchId id zápasu
     */
    private void addHattrickFine(int number, long playerId, long matchId) {
        FineDTO hattrickFine = new FineDTO(Config.HATTRICK_FINE_ID, "", 0, false);
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
