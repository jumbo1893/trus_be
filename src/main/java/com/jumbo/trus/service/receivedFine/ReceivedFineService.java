package com.jumbo.trus.service.receivedFine;

import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.dto.receivedfine.response.ReceivedFineResponse;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.dto.receivedfine.response.get.setup.ReceivedFineSetupResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.repository.ReceivedFineRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReceivedFineService {

    private final ReceivedFineRepository receivedFineRepository;
    private final ReceivedFineGetter receivedFineGetter;
    private final ReceivedFineUpdater receivedFineUpdater;

    public ReceivedFineDTO addFine(ReceivedFineDTO receivedFineDTO, AppTeamEntity appTeam) {
        return receivedFineUpdater.addFine(receivedFineDTO, appTeam);
    }

    public ReceivedFineResponse addFineToPlayer(ReceivedFineListDTO receivedFineListDTO, AppTeamEntity appTeam) {
        return receivedFineUpdater.addFineToPlayer(receivedFineListDTO, appTeam);
    }

    public ReceivedFineResponse addMultipleFines(ReceivedFineListDTO receivedFineListDTO, AppTeamEntity appTeam) {
        return receivedFineUpdater.addMultipleFines(receivedFineListDTO, appTeam);
    }

    public List<ReceivedFineDTO> getAll(ReceivedFineFilter receivedFineFilter) {
        return receivedFineGetter.getAll(receivedFineFilter);
    }

    public ReceivedFineDetailedResponse getAllDetailed(StatisticsFilter filter) {
        return receivedFineGetter.getAllDetailed(filter);
    }

    public ReceivedFineSetupResponse setupPlayers(ReceivedFineFilter receivedFineFilter) {
        return receivedFineGetter.setupPlayers(receivedFineFilter);
    }

    public List<ReceivedFineDTO> getAllForSetup(Long playerId, Long matchId, AppTeamEntity appTeam) {
        return receivedFineGetter.getAllForSetup(playerId, matchId, appTeam);
    }

    public int getReceivedFineCount(Long playerId, List<Long> matchIds, String fineName, long appTeamId) {
        List<ReceivedFineDTO> fines = receivedFineGetter.getReceivedFinesInMatchesByFineNameAndPlayer(playerId, matchIds, fineName, appTeamId);
        return fines.size();
    }

    public void deleteFine(Long fineId) {
        receivedFineRepository.deleteById(fineId);
    }

    public Integer getAtLeastNumberOfFineInHistory(Long playerId, String fineName, int fineNumber) {
        return receivedFineGetter.getAtLeastNumberOfFineInHistory(playerId, fineName, fineNumber);
    }

    public ReceivedFineDTO getFirstOccurrenceOfFine(Long playerId, String fineName) {
        return receivedFineGetter.getFirstOccurrenceOfFine(playerId, fineName);
    }



    /**
     * metoda vezme góly a dle toho přidá pokuty za góly a hattricky hráčům
     * @param matchId id zápasu
     * @param goalList seznam gólů
     */
    @Transactional
    public void rewriteFinesInDB(Long matchId, List<GoalDTO> goalList, AppTeamEntity appTeam) {
        receivedFineUpdater.rewriteFinesInDB(matchId, goalList, appTeam);
    }
}
