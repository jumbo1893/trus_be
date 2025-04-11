package com.jumbo.trus.service.receivedFine;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.dto.receivedfine.response.ReceivedFineResponse;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.repository.ReceivedFineRepository;
import com.jumbo.trus.entity.repository.specification.ReceivedFineSpecification;
import com.jumbo.trus.mapper.ReceivedFineMapper;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.NotificationService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.fine.FineService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReceivedFineUpdater {

    private final ReceivedFineRepository receivedFineRepository;
    private final ReceivedFineMapper receivedFineMapper;
    private final MatchService matchService;
    private final PlayerService playerService;
    private final FineService fineService;
    private final NotificationService notificationService;

    public ReceivedFineDTO addFine(ReceivedFineDTO receivedFineDTO, AppTeamEntity appTeam) {
        return receivedFineMapper.toDTO(saveFineToRepository(receivedFineDTO, appTeam));
    }

    public ReceivedFineResponse addFineToPlayer(ReceivedFineListDTO receivedFineListDTO, AppTeamEntity appTeam) {
        StringBuilder notificationText = new StringBuilder();
        ReceivedFineResponse receivedFineResponse = initializeReceivedFineResponse(receivedFineListDTO.getMatchId());
        receivedFineResponse.setPlayer(playerService.getPlayer(receivedFineListDTO.getPlayerId()).getName());
        for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
            receivedFineDTO.setMatchId(receivedFineListDTO.getMatchId());
            receivedFineDTO.setPlayerId(receivedFineListDTO.getPlayerId());
            notificationText.append(processAndSaveFine(receivedFineDTO, receivedFineResponse, false, appTeam));
        }
        notificationService.addNotification("V zápase " + receivedFineResponse.getMatch() + " byly přidány pokuty hráči " + receivedFineResponse.getPlayer(), notificationText.toString());
        return receivedFineResponse;
    }

    public ReceivedFineResponse addMultipleFines(ReceivedFineListDTO receivedFineListDTO, AppTeamEntity appTeam) {
        ReceivedFineResponse receivedFineResponse = initializeReceivedFineResponse(receivedFineListDTO.getMatchId());
        String notificationText = iterateMultiListOfReceivedFines(receivedFineListDTO, receivedFineResponse, appTeam);
        notificationService.addNotification("V zápase " + receivedFineResponse.getMatch(), notificationText);
        return receivedFineResponse;
    }

    /**
     * metoda vezme góly a dle toho přidá pokuty za góly a hattricky hráčům
     * @param matchId id zápasu
     * @param goalList seznam gólů
     */
    @Transactional
    public void rewriteFinesInDB(Long matchId, List<GoalDTO> goalList, AppTeamEntity appTeam) {
        receivedFineRepository.deleteGoalAndHattrickFinesFromMatch(matchId, Config.GOAL_FINE_ID, Config.HATTRICK_FINE_ID);
        for (GoalDTO goalDTO : goalList) {
            if (goalDTO.getGoalNumber() > 0) {
                addGoalFine(goalDTO.getGoalNumber(), goalDTO.getPlayerId(), matchId, appTeam);
                if (goalDTO.getGoalNumber() > 2) {
                    setAndAddHattrickFines(matchId, goalDTO.getGoalNumber()/3, goalDTO.getPlayerId(), appTeam);
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
    private void addGoalFine(int number, long playerId, long matchId, AppTeamEntity appTeam) {
        FineDTO goalFine = new FineDTO(Config.GOAL_FINE_ID, "", 0, false);
        ReceivedFineDTO receivedFine = new ReceivedFineDTO(number, goalFine, playerId, matchId);
        addFine(receivedFine, appTeam);
    }

    /**
     * metoda přidá pokutu za hattrick
     * @param number počet hattricků
     * @param playerId id hráče,
     * @param matchId id zápasu
     */
    private void addHattrickFine(int number, long playerId, long matchId, AppTeamEntity appTeam) {
        FineDTO hattrickFine = new FineDTO(Config.HATTRICK_FINE_ID, "", 0, false);
        ReceivedFineDTO receivedFine = new ReceivedFineDTO(number, hattrickFine, playerId, matchId);
        addFine(receivedFine, appTeam);
    }

    /**
     * metoda vyhodnotí, kteří hráči mají dostat pokutu (ostatní, než ten, co dal hattrick) a provolá metzodu na přidání pokut
     * @param numberOfHattricks počet hattricků
     * @param hattrickPlayerId id hráče, který dal hattrick
     * @param matchId id zápasu
     */
    private void setAndAddHattrickFines(long matchId, int numberOfHattricks, long hattrickPlayerId, AppTeamEntity appTeam) {
        List<Long> playerIds = playerService.convertPlayerListToPlayerIdList(playerService.getAllActive(true, appTeam.getId()));
        for (Long playerId : playerIds) {
            if (playerId != hattrickPlayerId) {
                addHattrickFine(numberOfHattricks, playerId, matchId, appTeam);
            }
        }
    }

    /**
     * @param receivedFineListDTO          request body, které obsahuje seznam pokut
     * @param receivedFineResponse instance objektu response, který pak vracíme
     *                                     metoda projde všechny hráče, kteří přišli v requestu a zároveň všechny pokuty, uloží je a obohatí response o výsledek
     */
    private String iterateMultiListOfReceivedFines(ReceivedFineListDTO receivedFineListDTO, ReceivedFineResponse receivedFineResponse, AppTeamEntity appTeam) {
        StringBuilder notificationPlayer = new StringBuilder("Byly navýšeny pokuty hráčům ");
        StringBuilder notificationFine = new StringBuilder();
        boolean firstFineProcessed = false;
        for (Long playerId : receivedFineListDTO.getPlayerIdList()) {
            processPlayerFines(receivedFineListDTO, receivedFineResponse, notificationFine, playerId, firstFineProcessed, appTeam);
            firstFineProcessed = true;
            String players = receivedFineListDTO.getPlayerIdList().stream()
                    .map(id -> playerService.getPlayer(id).getName())
                    .collect(Collectors.joining(", "));
            notificationPlayer.append(players);
            receivedFineResponse.addEditedPlayer();
        }
        trimTrailingComma(notificationPlayer);
        notificationPlayer.append(" o:");
        return notificationPlayer + "\n" + notificationFine;
    }

    private ReceivedFineDTO createReceivedFineDTO(ReceivedFineListDTO receivedFineListDTO, Long playerId, ReceivedFineDTO receivedFineDTO) {
        ReceivedFineDTO newReceivedFineDTO = new ReceivedFineDTO();
        newReceivedFineDTO.setMatchId(receivedFineListDTO.getMatchId());
        newReceivedFineDTO.setPlayerId(playerId);
        newReceivedFineDTO.setFineNumber(receivedFineDTO.getFineNumber());
        newReceivedFineDTO.setFine(receivedFineDTO.getFine());
        return newReceivedFineDTO;
    }

    private void trimTrailingComma(StringBuilder sb) {
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
    }

    private void processPlayerFines(ReceivedFineListDTO receivedFineListDTO, ReceivedFineResponse receivedFineResponse,
                                    StringBuilder notificationFine, Long playerId, boolean firstFineProcessed, AppTeamEntity appTeam) {
        for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
            ReceivedFineDTO newReceivedFineDTO = createReceivedFineDTO(receivedFineListDTO, playerId, receivedFineDTO);
            String fineNotification = processAndSaveFine(newReceivedFineDTO, receivedFineResponse, true, appTeam);
            if (!firstFineProcessed) {
                notificationFine.append(fineNotification);
            }
        }
    }

    private String processAndSaveFine(ReceivedFineDTO receivedFineDTO, ReceivedFineResponse receivedFineResponse, boolean multi, AppTeamEntity appTeam) {
        ReceivedFineDTO oldFine = getReceivedFineDtoByPlayerAndMatchAndFine(receivedFineDTO);
        if (shouldProcessFine(multi, receivedFineDTO, oldFine)) {
            receivedFineResponse.addFine(receivedFineDTO.getFineNumber());
            if (multi && oldFine != null) {
                receivedFineDTO.addFinesToFineNumber(oldFine.getFineNumber());
            }
            chooseIfRewriteDBOrCreateNewRow(receivedFineDTO, oldFine, appTeam);
            return generateFineNotification(receivedFineDTO);
        }

        return "";
    }

    private String generateFineNotification(ReceivedFineDTO receivedFineDTO) {
        return receivedFineDTO.getFine().getName() + ": " + receivedFineDTO.getFineNumber() + "\n";
    }

    private boolean isNecessaryToRewriteDB(ReceivedFineDTO newFine, ReceivedFineDTO oldFine) {
        return (oldFine != null && (newFine.getFineNumber() != oldFine.getFineNumber())) || (oldFine == null && (newFine.getFineNumber() != 0));
    }

    private void chooseIfRewriteDBOrCreateNewRow(ReceivedFineDTO receivedFineDTO, ReceivedFineDTO oldFine, AppTeamEntity appTeam) {
        if (oldFine != null) { //pokud existuje pokuta, musíme zapisovat pod jejím ID
            receivedFineDTO.setId(oldFine.getId());
        }
        saveFineToRepository(receivedFineDTO, appTeam);
    }

    /**
     * @param receivedFineDTO iterovaná pokuta
     * @return null pokud id neexistuje. Jinak se vrací objekt
     */
    private ReceivedFineDTO getReceivedFineDtoByPlayerAndMatchAndFine(ReceivedFineDTO receivedFineDTO) {
        ReceivedFineFilter filter = new ReceivedFineFilter(receivedFineDTO.getMatchId(), receivedFineDTO.getPlayerId(), receivedFineDTO.getFine().getId());
        ReceivedFineSpecification receivedFineSpecification = new ReceivedFineSpecification(filter);
        List<ReceivedFineDTO> filterList = receivedFineRepository.findAll(receivedFineSpecification, PageRequest.of(0, 1)).stream().map(receivedFineMapper::toDTO).toList();
        if (filterList.isEmpty()) {
            return null;
        }
        return filterList.get(0);
    }

    private boolean shouldProcessFine(boolean multi, ReceivedFineDTO receivedFineDTO, ReceivedFineDTO oldFine) {
        return (multi && receivedFineDTO.getFineNumber() != 0) || (!multi && isNecessaryToRewriteDB(receivedFineDTO, oldFine));
    }

    private ReceivedFineResponse initializeReceivedFineResponse(Long matchId) {
        return new ReceivedFineResponse(matchService.getMatch(matchId).getName());
    }

    private ReceivedFineEntity saveFineToRepository(ReceivedFineDTO receivedFineDTO, AppTeamEntity appTeam) {
        ReceivedFineEntity entity = receivedFineMapper.toEntity(receivedFineDTO);
        entity.setAppTeam(appTeam);
        mapPlayerMatchAndFine(entity, receivedFineDTO);
        return receivedFineRepository.save(entity);
    }

    private void mapPlayerMatchAndFine(ReceivedFineEntity receivedFine, ReceivedFineDTO receivedFineDTO) {
        receivedFine.setMatch(matchService.getMatchEntity(receivedFineDTO.getMatchId()));
        receivedFine.setPlayer(playerService.getPlayerEntity(receivedFineDTO.getPlayerId()));
        receivedFine.setFine(fineService.getFineEntity(receivedFineDTO.getFine().getId()));
    }

}
