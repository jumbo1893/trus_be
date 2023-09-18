package com.jumbo.trus.service;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.receivedfine.*;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.dto.receivedfine.response.ReceivedFineResponse;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.dto.receivedfine.response.get.setup.ReceivedFineSetupResponse;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.entity.repository.specification.ReceivedFineSpecification;
import com.jumbo.trus.entity.repository.specification.ReceivedFineStatsSpecification;
import com.jumbo.trus.mapper.ReceivedFineDetailedMapper;
import com.jumbo.trus.mapper.ReceivedFineMapper;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import com.jumbo.trus.service.order.OrderReceivedFineDetailedDTOByFineAmount;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReceivedFineService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private ReceivedFineMapper receivedFineMapper;

    @Autowired
    private ReceivedFineDetailedMapper receivedFineDetailedMapper;

    @Autowired
    private MatchService matchService;

    @Autowired
    private SeasonService seasonService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private FineService fineService;

    @Autowired
    private NotificationService notificationService;

    public ReceivedFineDTO addFine(ReceivedFineDTO receivedFineDTO) {
        ReceivedFineEntity entity = receivedFineMapper.toEntity(receivedFineDTO);
        mapPlayerMatchAndFine(entity, receivedFineDTO);
        ReceivedFineEntity savedEntity = receivedFineRepository.save(entity);
        return receivedFineMapper.toDTO(savedEntity);
    }

    public ReceivedFineResponse addFineToPlayer(ReceivedFineListDTO receivedFineListDTO) {
        StringBuilder notificationText = new StringBuilder();
        ReceivedFineResponse receivedFineResponse = new ReceivedFineResponse(matchRepository.getReferenceById(receivedFineListDTO.getMatchId()).getName());
        receivedFineResponse.setPlayer(playerRepository.getReferenceById(receivedFineListDTO.getPlayerId()).getName());
        for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
            receivedFineDTO.setMatchId(receivedFineListDTO.getMatchId());
            receivedFineDTO.setPlayerId(receivedFineListDTO.getPlayerId());
            notificationText.append(saveFineAndSetResponse(receivedFineDTO, receivedFineResponse, false));
        }
        notificationService.addNotification("V zápase " + receivedFineResponse.getMatch() + " byly přidány pokuty hráči " + receivedFineResponse.getPlayer(), notificationText.toString());
        return receivedFineResponse;
    }

    public ReceivedFineResponse addMultipleFines(ReceivedFineListDTO receivedFineListDTO) {
        ReceivedFineResponse receivedFineResponse = new ReceivedFineResponse(matchRepository.getReferenceById(receivedFineListDTO.getMatchId()).getName());
        String notificationText = iterateMultiListOfReceivedFines(receivedFineListDTO, receivedFineResponse);
        notificationService.addNotification("V zápase " + receivedFineResponse.getMatch(), notificationText);
        return receivedFineResponse;
    }

    public List<ReceivedFineDTO> getAll(ReceivedFineFilter receivedFineFilter) {
        ReceivedFineSpecification receivedFineSpecification = new ReceivedFineSpecification(receivedFineFilter);
        return receivedFineRepository.findAll(receivedFineSpecification, PageRequest.of(0, receivedFineFilter.getLimit())).stream().map(receivedFineMapper::toDTO).collect(Collectors.toList());
    }

    public ReceivedFineDetailedResponse getAllDetailed(StatisticsFilter filter) {
        ReceivedFineDetailedResponse receivedFineDetailedResponse = new ReceivedFineDetailedResponse();
        ReceivedFineStatsSpecification receivedFineSpecification = new ReceivedFineStatsSpecification(filter);
        List<ReceivedFineDetailedDTO> fineList = receivedFineRepository.findAll(receivedFineSpecification, PageRequest.of(0, filter.getLimit())).stream().map(receivedFineDetailedMapper::toDTO).toList();
        Set<Long> matchSet = new HashSet<>();
        Set<Long> playerSet = new HashSet<>();
        HashMap<Long, ReceivedFineDetailedDTO> matchMap = new HashMap<>();
        HashMap<Long, ReceivedFineDetailedDTO> playerMap = new HashMap<>();
        HashMap<Long, ReceivedFineDetailedDTO> fineMap = new HashMap<>();
        for (ReceivedFineDetailedDTO fine : fineList) {
            int fineNumber = fine.getFineNumber();
            int fineAmount = fine.getFineNumber()*fine.getFine().getAmount();
            receivedFineDetailedResponse.addFines(fineNumber);
            receivedFineDetailedResponse.addFineAmount(fineAmount);
            matchSet.add(fine.getMatch().getId());
            playerSet.add(fine.getPlayer().getId());
            if (filter.getDetailed() != null && filter.getDetailed()) {
                fine.setMatch(null);
                fine.setPlayer(null);
                if (!fineMap.containsKey(fine.getFine().getId())) {
                    fine.addFineAmount(fine.returnFineAmount());
                    fineMap.put(fine.getFine().getId(), fine);
                }
                else {
                    ReceivedFineDetailedDTO oldFine = fineMap.get(fine.getFine().getId());
                    oldFine.addFineNumber(fine.getFineNumber());
                    oldFine.addFineAmount(fine.returnFineAmount());
                    fineMap.put(fine.getFine().getId(), oldFine);
                }
            }
            else if (filter.getMatchStatsOrPlayerStats() != null && !filter.getMatchStatsOrPlayerStats()) {
                fine.setMatch(null);

                if (!playerMap.containsKey(fine.getPlayer().getId())) {
                    fine.addFineAmount(fine.returnFineAmount());
                    playerMap.put(fine.getPlayer().getId(), fine);
                }
                else {
                    ReceivedFineDetailedDTO oldFine = playerMap.get(fine.getPlayer().getId());
                    oldFine.addFineNumber(fine.getFineNumber());
                    oldFine.addFineAmount(fine.returnFineAmount());
                    playerMap.put(fine.getPlayer().getId(), oldFine);
                }
                fine.setFine(null);
            }
            else if (filter.getMatchStatsOrPlayerStats() != null) {
                fine.setPlayer(null);
                if (!matchMap.containsKey(fine.getMatch().getId())) {
                    fine.addFineAmount(fine.returnFineAmount());
                    matchMap.put(fine.getMatch().getId(), fine);
                }
                else {
                    ReceivedFineDetailedDTO oldFine = matchMap.get(fine.getMatch().getId());
                    oldFine.addFineNumber(fine.getFineNumber());
                    oldFine.addFineAmount(fine.returnFineAmount());
                    matchMap.put(fine.getMatch().getId(), oldFine);
                }
                fine.setFine(null);
            }
        }
        List<ReceivedFineDetailedDTO> returnFineList;
        if (filter.getMatchStatsOrPlayerStats() != null && filter.getMatchStatsOrPlayerStats()) {
            returnFineList = new ArrayList<>(matchMap.values().stream().toList());
        }
        else if (filter.getMatchStatsOrPlayerStats() != null) {
            returnFineList = new ArrayList<>(playerMap.values().stream().toList());
        }
        else {
            returnFineList = new ArrayList<>(fineList);
        }
        returnFineList.sort(new OrderReceivedFineDetailedDTOByFineAmount());
        receivedFineDetailedResponse.setFineList(returnFineList);
        receivedFineDetailedResponse.setMatchesCount(matchSet.size());
        receivedFineDetailedResponse.setPlayersCount(playerSet.size());
        return receivedFineDetailedResponse;
    }

    public ReceivedFineSetupResponse setupPlayers(ReceivedFineFilter receivedFineFilter) {

        PairSeasonMatch pairSeasonMatch = matchService.returnSeasonAndMatchByFilter(receivedFineFilter);
        SeasonDTO seasonDTO = pairSeasonMatch.getSeasonDTO();
        MatchDTO matchDTO = pairSeasonMatch.getMatchDTO();
        MatchFilter matchFilter = new MatchFilter();
        matchFilter.setSeasonId(seasonDTO.getId());
        List<MatchDTO> matchList =  matchService.getAll((matchFilter));

        List<PlayerDTO> playersInMatch = new ArrayList<>();
        List<PlayerDTO> otherPlayers = new ArrayList<>();

        if(matchDTO != null) {
            playersInMatch = matchService.getPlayerListByFilteredByFansByMatchId(matchDTO.getId(), false);
            otherPlayers = playerService.getAllActive(true);
            otherPlayers.removeAll(playersInMatch); // Odstraníme otherPlayers všechny hodnoty, které jsou v playersInMatch
        }
        return new ReceivedFineSetupResponse(matchDTO, seasonDTO, playersInMatch, otherPlayers, matchList);
    }

    public List<ReceivedFineDTO> getAllForSetup(Long playerId, Long matchId) {
        if(playerId == null || matchId == null) {
            throw new EntityNotFoundException();
        }
        List<ReceivedFineDTO> receivedFines = getAll(new ReceivedFineFilter(matchId, playerId));
        List<Long> idList = getListOfFineIdsFromReceivedFines(receivedFines);
        List<FineDTO> fineDTOS;
        if(idList.isEmpty()) {
            fineDTOS = fineService.getAll(1000);
        }
        else {
            fineDTOS = fineService.getAllOtherFines(idList);
        }
        receivedFines.addAll(makeReceivedFineSetupListFromFineList(fineDTOS, playerId, matchId));
        return receivedFines;
    }

    private List<Long> getListOfFineIdsFromReceivedFines(List<ReceivedFineDTO> receivedFines) {
        List<Long> returnList = new ArrayList<>();
        for (ReceivedFineDTO receivedFine : receivedFines) {
            returnList.add(receivedFine.getFine().getId());
        }
        return returnList;
    }

    private List<ReceivedFineDTO> makeReceivedFineSetupListFromFineList(List<FineDTO> fineDTOS, Long playerId, Long matchId) {
        List<ReceivedFineDTO> receivedFineDTOS = new ArrayList<>();
        for (FineDTO fine : fineDTOS) {
            receivedFineDTOS.add(new ReceivedFineDTO(0, fine, playerId, matchId));
        }
        return receivedFineDTOS;
    }

    public void deleteFine(Long fineId) {
        receivedFineRepository.deleteById(fineId);
    }

    private void mapPlayerMatchAndFine(ReceivedFineEntity receivedFine, ReceivedFineDTO receivedFineDTO) {
        receivedFine.setMatch(matchRepository.getReferenceById(receivedFineDTO.getMatchId()));
        receivedFine.setPlayer(playerRepository.getReferenceById(receivedFineDTO.getPlayerId()));
        receivedFine.setFine(fineRepository.getReferenceById(receivedFineDTO.getFine().getId()));
    }

    private void saveFineToRepository(ReceivedFineDTO receivedFineDTO) {
        ReceivedFineEntity entity = receivedFineMapper.toEntity(receivedFineDTO);
        receivedFineRepository.save(entity);
    }

    private boolean isNecessaryToRewriteDB(ReceivedFineDTO newFine, ReceivedFineDTO oldFine) {
        return (oldFine != null && (newFine.getFineNumber() != oldFine.getFineNumber())) || (oldFine == null && (newFine.getFineNumber() != 0));
    }


    /**
     * @param receivedFineDTO              iterovaná pokuta
     * @param receivedFineResponse instance objektu response, který pak vracíme
     * @param multi                        true, pokud se jedná o multipokutu
     *                                     Pokud je počet pokut vyšší než 0, tak metoda zjistí, zda má přidat pokutu již k uložené pokutě v db (dle id) či založit novou. Následně přičte počet pokut k response
     */
    private String saveFineAndSetResponse(ReceivedFineDTO receivedFineDTO, ReceivedFineResponse receivedFineResponse, boolean multi) {
        String notificationResponse = "";
        ReceivedFineDTO oldFine = getReceivedFineDtoByPlayerAndMatchAndFine(receivedFineDTO);
        if (multi && receivedFineDTO.getFineNumber() != 0) {
            receivedFineResponse.addFine(receivedFineDTO.getFineNumber());
            if (oldFine != null) {
                receivedFineDTO.addFinesToFineNumber(oldFine.getFineNumber());
            }
            chooseIfRewriteDBOrCreateNewRow(receivedFineDTO, oldFine);
            notificationResponse = receivedFineDTO.getFine().getName() + ": " + receivedFineDTO.getFineNumber() + "\n";

        } else if (!multi && isNecessaryToRewriteDB(receivedFineDTO, oldFine)) {
            receivedFineResponse.addFine(receivedFineDTO.getFineNumber());
            chooseIfRewriteDBOrCreateNewRow(receivedFineDTO, oldFine);
            notificationResponse = receivedFineDTO.getFine().getName() + ": " + receivedFineDTO.getFineNumber() + "\n";
        }
        return notificationResponse;
    }

    private void chooseIfRewriteDBOrCreateNewRow(ReceivedFineDTO receivedFineDTO, ReceivedFineDTO oldFine) {
        if (oldFine != null) { //pokud existuje pokuta, musíme zapisovat pod jejím ID
            receivedFineDTO.setId(oldFine.getId());
        }
        saveFineToRepository(receivedFineDTO);
    }


    /**
     * @param receivedFineListDTO          request body, které obsahuje seznam pokut
     * @param receivedFineResponse instance objektu response, který pak vracíme
     *                                     metoda projde všechny hráče, kteří přišli v requestu a zároveň všechny pokuty, uloží je a obohatí response o výsledek
     */
    private String iterateMultiListOfReceivedFines(ReceivedFineListDTO receivedFineListDTO, ReceivedFineResponse
            receivedFineResponse) {
        StringBuilder notificationPlayer = new StringBuilder();
        StringBuilder notificationFine = new StringBuilder();
        boolean allFinesWroteToNotification = false;
        notificationPlayer.append("Byly navýšeny pokuty hráčům ");
        for (Long playerId : receivedFineListDTO.getPlayerIdList()) {
            for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
                ReceivedFineDTO newReceivedFineDTO = new ReceivedFineDTO();
                newReceivedFineDTO.setMatchId(receivedFineListDTO.getMatchId());
                newReceivedFineDTO.setPlayerId(playerId);
                newReceivedFineDTO.setFineNumber(receivedFineDTO.getFineNumber());
                newReceivedFineDTO.setFine(receivedFineDTO.getFine());
                if (!allFinesWroteToNotification) {
                    notificationFine.append(saveFineAndSetResponse(newReceivedFineDTO, receivedFineResponse, true));
                }
            }
            allFinesWroteToNotification = true;
            notificationPlayer.append(playerRepository.getReferenceById(playerId).getName()).append(", ");
            receivedFineResponse.addEditedPlayer();
        }
        int lastChar = notificationPlayer.length() - 1;
        notificationPlayer.deleteCharAt(lastChar);
        notificationPlayer.append(" o:");
        return notificationPlayer+"\n"+notificationFine;
    }

    /**
     * @param receivedFineDTO iterovaná pokuta
     * @return null pokud id neexistuje. Jinak se vrací objekt
     */
    private ReceivedFineDTO getReceivedFineDtoByPlayerAndMatchAndFine(ReceivedFineDTO receivedFineDTO) {
        ReceivedFineFilter filter = new ReceivedFineFilter(receivedFineDTO.getMatchId(), receivedFineDTO.getPlayerId(), receivedFineDTO.getFine().getId());
        ReceivedFineSpecification receivedFineSpecification = new ReceivedFineSpecification(filter);
        List<ReceivedFineDTO> filterList = receivedFineRepository.findAll(receivedFineSpecification, PageRequest.of(0, 1)).stream().map(receivedFineMapper::toDTO).collect(Collectors.toList());
        if (filterList.isEmpty()) {
            return null;
        }
        return filterList.get(0);
    }
}
