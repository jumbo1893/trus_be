package com.jumbo.trus.service;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.goal.multi.GoalListDTO;
import com.jumbo.trus.dto.goal.response.GoalMultiAddResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalSetupResponse;
import com.jumbo.trus.entity.GoalEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.GoalFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.entity.repository.specification.GoalSpecification;
import com.jumbo.trus.mapper.*;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import com.jumbo.trus.service.helper.DetailedResponseHelper;
import com.jumbo.trus.service.player.PlayerService;
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

    private final GoalRepository goalRepository;
    private final GoalMapper goalMapper;
    private final GoalSetupMapper goalSetupMapper;
    private final MatchService matchService;
    private final PlayerService playerService;
    private final ReceivedFineService receivedFineService;
    private final NotificationService notificationService;
    private final DetailedResponseHelper detailedResponseHelper;

    /**
     * metoda napamuje hráče a zápas z přepravky ke gólu a uloží ho do DB
     * @param goalDTO Gól, který přijde z FE
     * @return Gól z DB
     */
    public GoalDTO addGoal(GoalDTO goalDTO, AppTeamEntity appTeam) {
        return goalMapper.toDTO(saveGoalToRepository(goalDTO, appTeam));
    }

    /**
     * Projde seznam gólů u hráčů a v případě změny zapíše změny do db. Počet změn následně vypíše
     * @param goalListDTO List ve formě přepravky GoalListDTO, který přijde z FE. Obsahuje jak změněné počty gólů u hráčů u konkrétního zápasu, tak může obsahovat i nezměněné počty
     * @return GoalMultiAddResponse - vypsaný počet změn v DB
     */
    @Transactional
    public GoalMultiAddResponse addMultipleGoal(GoalListDTO goalListDTO, AppTeamEntity appTeam) {
        StringBuilder newGoalNotification = new StringBuilder();
        StringBuilder newAssistNotification = new StringBuilder();
        GoalMultiAddResponse goalMultiAddResponse = new GoalMultiAddResponse();
        goalMultiAddResponse.setMatch(matchService.getMatch(goalListDTO.getMatchId()).getName());
        for (GoalDTO goalDTO : goalListDTO.getGoalList()) {
            processGoal(goalDTO, goalListDTO.getMatchId(), newGoalNotification, newAssistNotification, goalMultiAddResponse, appTeam);
        }
        if (goalListDTO.isRewriteToFines()) {
            receivedFineService.rewriteFinesInDB(goalListDTO.getMatchId(), goalListDTO.getGoalList(), appTeam);
        }
        notificationService.addNotification("Přidány góly/asistence v zápase " + goalMultiAddResponse.getMatch(), newGoalNotification+newAssistNotification.toString());
        return goalMultiAddResponse;
    }

    private void processGoal(GoalDTO goalDTO, Long matchId, StringBuilder newGoalNotification, StringBuilder newAssistNotification, GoalMultiAddResponse goalMultiAddResponse, AppTeamEntity appTeam) {
        goalDTO.setMatchId(matchId);
        GoalDTO oldGoal = getGoalDtoByPlayerAndMatch(matchId, goalDTO.getPlayerId());
        int goalDiff = checkGoalDiff(oldGoal, goalDTO);
        int assistDiff = checkAssistDiff(oldGoal, goalDTO);
        if (isNeededToRewriteGoal(oldGoal, goalDiff, assistDiff)) {
            goalDTO.setId(oldGoal.getId());
            saveGoalToRepository(goalDTO, appTeam);
            setMultiAddResponse(goalMultiAddResponse, goalDiff, assistDiff);
            setMultiGoalNotification(newGoalNotification, newAssistNotification, goalDiff, assistDiff, goalDTO);
        } else if (isNeededToAddGoalFirstGoal(oldGoal, goalDTO)) {
            saveGoalToRepository(goalDTO, appTeam);
            setMultiAddResponse(goalMultiAddResponse, goalDTO.getGoalNumber(), goalDTO.getAssistNumber());
            setMultiGoalNotification(newGoalNotification, newAssistNotification, goalDTO.getGoalNumber(), goalDTO.getAssistNumber(), goalDTO);
        }
    }

    private boolean isNeededToRewriteGoal(GoalDTO oldGoal, int goalDiff, int assistDiff) {
        return oldGoal != null && (goalDiff != 0 || assistDiff != 0);
    }

    private void setMultiAddResponse(GoalMultiAddResponse goalMultiAddResponse, int goals, int assists) {
        goalMultiAddResponse.addGoals(goals);
        goalMultiAddResponse.addAssists(assists);
    }

    private void setMultiGoalNotification(StringBuilder newGoalNotification, StringBuilder newAssistNotification, int goals, int assists, GoalDTO goalDTO) {
        String playerName = playerService.getPlayer(goalDTO.getPlayerId()).getName();
        if (goals != 0) {
            newGoalNotification.append(playerName).append(" vstřelil gólů: ").append(goalDTO.getGoalNumber()).append("\n");
        }
        if (assists != 0) {
            newAssistNotification.append(playerName).append(" má přihrávek: ").append(goalDTO.getAssistNumber()).append("\n");
        }
    }

    /**
     *
     * @param oldGoal gol z DB
     * @param goalDTO gól z FE
     * @return Vrátí zda je nutné přidat gól do db (je nesmysl tam dávat že někdo dal 0 gólů to je automatika)
     */
    private boolean isNeededToAddGoalFirstGoal(GoalDTO oldGoal, GoalDTO goalDTO) {
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
       return new GoalDetailedResponse(detailedResponseHelper.getAllDetailed(filter, DetailedResponseHelper.DetailedType.GOAL));
    }


    /**
     * Metoda vrátí setup gólů, který se použije v objektu. Jedná se o počet vstřelených gólů a asistencí v daném zápase pro dané hráče dle filtru
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

    public GoalDTO getGoalkeeperWithMostPointsInMatch(Long playerId) {
        return goalRepository.findGoalkeeperWithMostPointsInMatch(playerId)
                .map(goalMapper::toDTO)
                .orElse(null);
    }

    private void mapPlayerAndMatch(GoalEntity goal, GoalDTO goalDTO) {
        goal.setMatch(matchService.getMatchEntity(goalDTO.getMatchId()));
        goal.setPlayer(playerService.getPlayerEntity(goalDTO.getPlayerId()));
    }

    private GoalEntity saveGoalToRepository(GoalDTO goalDTO, AppTeamEntity appTeamEntity) {
        GoalEntity entity = goalMapper.toEntity(goalDTO);
        entity.setAppTeam(appTeamEntity);
        mapPlayerAndMatch(entity, goalDTO);
        return goalRepository.save(entity);
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
