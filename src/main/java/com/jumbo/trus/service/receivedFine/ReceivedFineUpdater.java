package com.jumbo.trus.service.receivedFine;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.dto.receivedfine.response.ReceivedFineResponse;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.mapper.ReceivedFineMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.repository.specification.ReceivedFineSpecification;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.notification.push.maker.FineNotificationMaker;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceivedFineUpdater {

    private final ReceivedFineRepository receivedFineRepository;
    private final ReceivedFineMapper receivedFineMapper;
    private final PlayerService playerService;
    private final FineService fineService;
    private final NotificationService notificationService;
    private final FineNotificationMaker fineNotificationMaker;
    private final MatchRepository matchRepository;
    private final OutboxEventService outboxEventService;

    @Transactional
    public ReceivedFineDTO addFine(ReceivedFineDTO receivedFineDTO, AppTeamEntity appTeam) {
       ReceivedFineEntity receivedFine = saveFineToRepository(receivedFineDTO, null, appTeam);
        return receivedFineMapper.toDTO(receivedFine);
    }

    @Transactional
    public ReceivedFineResponse addFineToPlayer(ReceivedFineListDTO receivedFineListDTO, AppTeamEntity appTeam) {
        StringBuilder notificationText = new StringBuilder();
        ReceivedFineResponse receivedFineResponse = initializeReceivedFineResponse(receivedFineListDTO.getMatchId());
        receivedFineResponse.setPlayer(playerService.getPlayer(receivedFineListDTO.getPlayerId()).getName());
        Set<Long> receivedFineIds = new HashSet<>();
        Set<Long> fineIds = new HashSet<>();
        for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
            receivedFineDTO.setMatchId(receivedFineListDTO.getMatchId());
            receivedFineDTO.setPlayerId(receivedFineListDTO.getPlayerId());
            ReceivedFineDTO processedReceivedFine = processAndSaveFine(receivedFineDTO, receivedFineResponse, false, appTeam);
            if (processedReceivedFine != null) {
                notificationText.append(generateFineNotification(processedReceivedFine));
                receivedFineIds.add(processedReceivedFine.getId());
                fineIds.add(processedReceivedFine.getFine().getId());
            }
        }
        notificationService.addNotification("V zápase " + receivedFineResponse.getMatch() + " byly přidány pokuty hráči " + receivedFineResponse.getPlayer(), notificationText.toString());
        outboxEventService.createEvent(OutboxEventType.RECEIVED_FINE_CHANGED, OutboxAggregateType.RECEIVED_FINE, null,
                OutboxEventPayloadFactory.receivedFinesChanged(
                        receivedFineListDTO.getMatchId(),
                        matchRepository.findSeasonIdByMatchId(receivedFineListDTO.getMatchId()),
                        Set.of(receivedFineListDTO.getPlayerId()),
                        receivedFineIds,
                        fineIds));
        return receivedFineResponse;
    }

    @Transactional
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
        Set<Long> playersWithFines = receivedFineRepository.findPlayersByFineIdsAndMatchId(matchId, Config.GOAL_FINE_ID, Config.HATTRICK_FINE_ID);
        Set<Long> playersWithNewFines = new HashSet<>();
        Set<Long> receivedFineIds = new HashSet<>();
        Set<Long> fineIds = new HashSet<>();
        receivedFineRepository.deleteGoalAndHattrickFinesFromMatch(matchId, Config.GOAL_FINE_ID, Config.HATTRICK_FINE_ID);
        for (GoalDTO goalDTO : goalList) {
            if (goalDTO.getGoalNumber() > 0) {
                receivedFineIds.add(addGoalFine(goalDTO.getGoalNumber(), goalDTO.getPlayerId(), matchId, appTeam));
                playersWithNewFines.add(goalDTO.getPlayerId());
                fineIds.add(Config.GOAL_FINE_ID);
                if (goalDTO.getGoalNumber() > 2) {
                    receivedFineIds.addAll(setAndAddHattrickFines(matchId, goalDTO.getGoalNumber()/3, goalDTO.getPlayerId(), appTeam));
                    playersWithNewFines.addAll(playerService.convertPlayerListToPlayerIdList(playerService.getAllActive(true, appTeam.getId())));
                    fineIds.add(Config.HATTRICK_FINE_ID);
                }
            }
        }
        playersWithFines.addAll(playersWithNewFines);
        outboxEventService.createEvent(OutboxEventType.RECEIVED_FINE_CHANGED, OutboxAggregateType.RECEIVED_FINE, null,
                OutboxEventPayloadFactory.receivedFinesChanged(
                        matchId,
                        matchRepository.findSeasonIdByMatchId(matchId),
                        playersWithFines,
                        receivedFineIds,
                       fineIds));
    }

    /**
     * metoda přidá pokutu za gól
     * @param number počet gólů
     * @param playerId id hráče
     * @param matchId id zápasu
     */
    private Long addGoalFine(int number, long playerId, long matchId, AppTeamEntity appTeam) {
        FineDTO goalFine = new FineDTO(Config.GOAL_FINE_ID, "", 0, false);
        ReceivedFineDTO receivedFine = new ReceivedFineDTO(number, goalFine, playerId, matchId);
        return addFine(receivedFine, appTeam).getId();
    }

    /**
     * metoda přidá pokutu za hattrick
     * @param number počet hattricků
     * @param playerId id hráče,
     * @param matchId id zápasu
     */
    private Long addHattrickFine(int number, long playerId, long matchId, AppTeamEntity appTeam) {
        FineDTO hattrickFine = new FineDTO(Config.HATTRICK_FINE_ID, "", 0, false);
        ReceivedFineDTO receivedFine = new ReceivedFineDTO(number, hattrickFine, playerId, matchId);
        return addFine(receivedFine, appTeam).getId();
    }

    /**
     * metoda vyhodnotí, kteří hráči mají dostat pokutu (ostatní, než ten, co dal hattrick) a provolá metzodu na přidání pokut
     * @param numberOfHattricks počet hattricků
     * @param hattrickPlayerId id hráče, který dal hattrick
     * @param matchId id zápasu
     */
    private Set<Long> setAndAddHattrickFines(long matchId, int numberOfHattricks, long hattrickPlayerId, AppTeamEntity appTeam) {
        List<Long> playerIds = playerService.convertPlayerListToPlayerIdList(playerService.getAllActive(true, appTeam.getId()));
        Set<Long> ids = new HashSet<>();
        for (Long playerId : playerIds) {
            if (playerId != hattrickPlayerId) {
                ids.add(addHattrickFine(numberOfHattricks, playerId, matchId, appTeam));
            }
        }
        return ids;
    }

    /**
     * @param receivedFineListDTO          request body, které obsahuje seznam pokut
     * @param receivedFineResponse instance objektu response, který pak vracíme
     *                                     metoda projde všechny hráče, kteří přišli v requestu a zároveň všechny pokuty, uloží je a obohatí response o výsledek
     */
    @Transactional
    private String iterateMultiListOfReceivedFines(ReceivedFineListDTO receivedFineListDTO, ReceivedFineResponse receivedFineResponse, AppTeamEntity appTeam) {
        StringBuilder notificationPlayer = new StringBuilder("Byly navýšeny pokuty hráčům ");
        StringBuilder notificationFine = new StringBuilder();
        Set<Long> receivedFineIds = new HashSet<>();
        Set<Long> fineIds = new HashSet<>();
        boolean firstFineProcessed = false;
        for (Long playerId : receivedFineListDTO.getPlayerIdList()) {
            processPlayerFines(receivedFineListDTO, receivedFineResponse, notificationFine, playerId, firstFineProcessed, appTeam, receivedFineIds, fineIds);
            firstFineProcessed = true;
        }
        String players = receivedFineListDTO.getPlayerIdList().stream()
                .map(id -> playerService.getPlayer(id).getName())
                .collect(Collectors.joining(", "));
        notificationPlayer.append(players);
        receivedFineResponse.addEditedPlayer();
        notificationPlayer.append(" o:");
        outboxEventService.createEvent(OutboxEventType.RECEIVED_FINE_CHANGED, OutboxAggregateType.RECEIVED_FINE, null,
                OutboxEventPayloadFactory.receivedFinesChanged(
                        receivedFineListDTO.getMatchId(),
                        matchRepository.findSeasonIdByMatchId(receivedFineListDTO.getMatchId()),
                        new HashSet<>(receivedFineListDTO.getPlayerIdList()),
                        receivedFineIds,
                        fineIds));
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

    private void processPlayerFines(ReceivedFineListDTO receivedFineListDTO, ReceivedFineResponse receivedFineResponse,
                                   StringBuilder notificationFine, Long playerId, boolean firstFineProcessed, AppTeamEntity appTeam, Set<Long> receivedFineIds, Set<Long> fineIds) {
        for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
            ReceivedFineDTO newReceivedFineDTO = createReceivedFineDTO(receivedFineListDTO, playerId, receivedFineDTO);
            ReceivedFineDTO processedReceivedFine = processAndSaveFine(newReceivedFineDTO, receivedFineResponse, true, appTeam);
            String fineNotification = "";
            if (processedReceivedFine != null) {
                fineNotification = generateFineNotification(processedReceivedFine);
                receivedFineIds.add(processedReceivedFine.getId());
                fineIds.add(processedReceivedFine.getFine().getId());
            }
            if (!firstFineProcessed) {
                notificationFine.append(fineNotification);
            }
        }
    }

    private ReceivedFineDTO processAndSaveFine(ReceivedFineDTO receivedFineDTO, ReceivedFineResponse receivedFineResponse, boolean multi, AppTeamEntity appTeam) {
        ReceivedFineDTO oldFine = getReceivedFineDtoByPlayerAndMatchAndFine(receivedFineDTO);
        if (shouldProcessFine(multi, receivedFineDTO, oldFine)) {
            receivedFineResponse.addFine(receivedFineDTO.getFineNumber());
            if (multi && oldFine != null) {
                receivedFineDTO.addFinesToFineNumber(oldFine.getFineNumber());
            }
            return receivedFineMapper.toDTO(chooseIfRewriteDBOrCreateNewRow(receivedFineDTO, oldFine, appTeam));
        }

        return null;
    }

    private String generateFineNotification(ReceivedFineDTO receivedFineDTO) {
        return receivedFineDTO.getFine().getName() + ": " + receivedFineDTO.getFineNumber() + "\n";
    }

    private boolean isNecessaryToRewriteDB(ReceivedFineDTO newFine, ReceivedFineDTO oldFine) {
        return (oldFine != null && (newFine.getFineNumber() != oldFine.getFineNumber())) || (oldFine == null && (newFine.getFineNumber() != 0));
    }

    private ReceivedFineEntity chooseIfRewriteDBOrCreateNewRow(ReceivedFineDTO receivedFineDTO, ReceivedFineDTO oldFine, AppTeamEntity appTeam) {
        if (oldFine != null) { //pokud existuje pokuta, musíme zapisovat pod jejím ID
            receivedFineDTO.setId(oldFine.getId());
        }
        return saveFineToRepository(receivedFineDTO, oldFine, appTeam);
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
        return new ReceivedFineResponse(getMatchEntity(matchId).getName());
    }

    private ReceivedFineEntity saveFineToRepository(ReceivedFineDTO receivedFineDTO, ReceivedFineDTO oldFine, AppTeamEntity appTeam) {
        ReceivedFineEntity entity = receivedFineMapper.toEntity(receivedFineDTO);
        entity.setAppTeam(appTeam);
        mapPlayerMatchAndFine(entity, receivedFineDTO);
        ReceivedFineEntity savedEntity = receivedFineRepository.save(entity);
        fineNotificationMaker.sendFineNotify(entity, oldFine);
        return savedEntity;
    }

    private void mapPlayerMatchAndFine(ReceivedFineEntity receivedFine, ReceivedFineDTO receivedFineDTO) {
        receivedFine.setMatch(getMatchEntity(receivedFineDTO.getMatchId()));
        receivedFine.setPlayer(playerService.getPlayerEntity(receivedFineDTO.getPlayerId()));
        receivedFine.setFine(fineService.getFineEntity(receivedFineDTO.getFine().getId()));
    }

    private MatchEntity getMatchEntity(long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
    }
}
