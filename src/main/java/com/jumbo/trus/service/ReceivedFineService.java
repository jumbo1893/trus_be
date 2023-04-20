package com.jumbo.trus.service;

import com.jumbo.trus.dto.beer.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.BeerDetailedResponse;
import com.jumbo.trus.dto.receivedFine.*;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.entity.repository.specification.BeerSpecification;
import com.jumbo.trus.entity.repository.specification.ReceivedFineSpecification;
import com.jumbo.trus.mapper.ReceivedFineDetailedMapper;
import com.jumbo.trus.mapper.ReceivedFineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public ReceivedFineDTO addFine(ReceivedFineDTO receivedFineDTO) {
        ReceivedFineEntity entity = receivedFineMapper.toEntity(receivedFineDTO);
        mapPlayerMatchAndFine(entity, receivedFineDTO);
        ReceivedFineEntity savedEntity = receivedFineRepository.save(entity);
        return receivedFineMapper.toDTO(savedEntity);
    }

    public ReceivedFineResponse addFineToPlayer(ReceivedFineListDTO receivedFineListDTO) {
        ReceivedFineResponse receivedFineResponse = new ReceivedFineResponse(matchRepository.getReferenceById(receivedFineListDTO.getMatchId()).getName());
        receivedFineResponse.setPlayer(playerRepository.getReferenceById(receivedFineListDTO.getPlayerId()).getName());
        for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
            receivedFineDTO.setMatchId(receivedFineListDTO.getMatchId());
            receivedFineDTO.setPlayerId(receivedFineListDTO.getPlayerId());
            saveFineAndSetResponse(receivedFineDTO, receivedFineResponse, false);
        }
        return receivedFineResponse;
    }

    public ReceivedFineResponse addMultipleFines(ReceivedFineListDTO receivedFineListDTO) {
        ReceivedFineResponse receivedFineResponse = new ReceivedFineResponse(matchRepository.getReferenceById(receivedFineListDTO.getMatchId()).getName());
        iterateMultiListOfReceivedFines(receivedFineListDTO, receivedFineResponse);
        return receivedFineResponse;
    }

    public List<ReceivedFineDTO> getAll(ReceivedFineFilter receivedFineFilter) {
        ReceivedFineSpecification receivedFineSpecification = new ReceivedFineSpecification(receivedFineFilter);
        return receivedFineRepository.findAll(receivedFineSpecification, PageRequest.of(0, receivedFineFilter.getLimit())).stream().map(receivedFineMapper::toDTO).collect(Collectors.toList());
    }

    public ReceivedFineDetailedResponse getAllDetailed(ReceivedFineFilter receivedFineFilter) {
        ReceivedFineDetailedResponse receivedFineDetailedResponse = new ReceivedFineDetailedResponse();
        ReceivedFineSpecification receivedFineSpecification = new ReceivedFineSpecification(receivedFineFilter);
        List<ReceivedFineDetailedDTO> fineList = receivedFineRepository.findAll(receivedFineSpecification, PageRequest.of(0, receivedFineFilter.getLimit())).stream().map(receivedFineDetailedMapper::toDTO).collect(Collectors.toList());
        Set<Long> matchSet = new HashSet<>();
        Set<Long> playerSet = new HashSet<>();
        for (ReceivedFineDetailedDTO fine : fineList) {
            receivedFineDetailedResponse.addFines(fine.getFineNumber());
            receivedFineDetailedResponse.addFineAmount(fine.getFineNumber()*fine.getFine().getAmount());
            matchSet.add(fine.getMatch().getId());
            playerSet.add(fine.getPlayer().getId());
        }
        receivedFineDetailedResponse.setFineList(fineList);
        receivedFineDetailedResponse.setMatchesCount(matchSet.size());
        receivedFineDetailedResponse.setPlayersCount(playerSet.size());
        return receivedFineDetailedResponse;
    }

    public void deleteFine(Long fineId) {
        fineRepository.deleteById(fineId);
    }

    private void mapPlayerMatchAndFine(ReceivedFineEntity receivedFine, ReceivedFineDTO receivedFineDTO) {
        receivedFine.setMatch(matchRepository.getReferenceById(receivedFineDTO.getMatchId()));
        receivedFine.setPlayer(playerRepository.getReferenceById(receivedFineDTO.getPlayerId()));
        receivedFine.setFine(fineRepository.getReferenceById(receivedFineDTO.getFineId()));
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
    private void saveFineAndSetResponse(ReceivedFineDTO receivedFineDTO, ReceivedFineResponse receivedFineResponse, boolean multi) {
        ReceivedFineDTO oldFine = getReceivedFineDtoByPlayerAndMatchAndFine(receivedFineDTO);
        if (multi && receivedFineDTO.getFineNumber() != 0) {
            receivedFineResponse.addFine(receivedFineDTO.getFineNumber());
            if (oldFine != null) {
                receivedFineDTO.addFinesToFineNumber(oldFine.getFineNumber());
            }
            chooseIfRewriteDBOrCreateNewRow(receivedFineDTO, oldFine);

        } else if (!multi && isNecessaryToRewriteDB(receivedFineDTO, oldFine)) {
            receivedFineResponse.addFine(receivedFineDTO.getFineNumber());
            chooseIfRewriteDBOrCreateNewRow(receivedFineDTO, oldFine);
        }
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
    private void iterateMultiListOfReceivedFines(ReceivedFineListDTO receivedFineListDTO, ReceivedFineResponse
            receivedFineResponse) {
        for (Long playerId : receivedFineListDTO.getPlayerIdList()) {
            for (ReceivedFineDTO receivedFineDTO : receivedFineListDTO.getFineList()) {
                ReceivedFineDTO newReceivedFineDTO = new ReceivedFineDTO();
                newReceivedFineDTO.setMatchId(receivedFineListDTO.getMatchId());
                newReceivedFineDTO.setPlayerId(playerId);
                newReceivedFineDTO.setFineNumber(receivedFineDTO.getFineNumber());
                newReceivedFineDTO.setFineId(receivedFineDTO.getFineId());
                saveFineAndSetResponse(newReceivedFineDTO, receivedFineResponse, true);
            }
            receivedFineResponse.addEditedPlayer();
        }
    }

    /**
     * @param receivedFineDTO iterovaná pokuta
     * @return null pokud id neexistuje. Jinak se vrací objekt
     */
    private ReceivedFineDTO getReceivedFineDtoByPlayerAndMatchAndFine(ReceivedFineDTO receivedFineDTO) {
        ReceivedFineFilter filter = new ReceivedFineFilter(receivedFineDTO.getMatchId(), receivedFineDTO.getPlayerId(), receivedFineDTO.getFineId());
        ReceivedFineSpecification receivedFineSpecification = new ReceivedFineSpecification(filter);
        List<ReceivedFineDTO> filterList = receivedFineRepository.findAll(receivedFineSpecification, PageRequest.of(0, 1)).stream().map(receivedFineMapper::toDTO).collect(Collectors.toList());
        if (filterList.isEmpty()) {
            return null;
        }
        return filterList.get(0);
    }
}
