package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.FineMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.FineRepository;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.receivedFine.ReceivedFineUpdater;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchResultFineService {

    private final ReceivedFineRepository receivedFineRepository;
    private final FineRepository fineRepository;
    private final PlayerRepository playerRepository;
    private final ReceivedFineUpdater receivedFineService;
    private final PlayerService playerService;
    private final PlayerMapper playerMapper;
    private final FineMapper fineMapper;

    @Transactional
    public void rewriteAutomaticFines(MatchEntity match, AppTeamEntity appTeam) {
        deleteExistingAutomaticResultFines(match.getId(), appTeam.getId());

        if (match.getHomeGoalNumber() == null || match.getAwayGoalNumber() == null) {
            return;
        }

        List<PlayerEntity> playingPlayers = match.getPlayerList().stream()
                .filter(player -> !player.isFan())
                .toList();

        List<Long> playingPlayerIds = playingPlayers.stream()
                .map(PlayerEntity::getId)
                .toList();

        List<PlayerEntity> absentPlayers = playerRepository
                .getAllByActive(true, appTeam.getId())
                .stream()
                .filter(player -> !playingPlayerIds.contains(player.getId()))
                .toList();

        int ourScore = match.isHome()
                ? match.getHomeGoalNumber()
                : match.getAwayGoalNumber();

        int opponentScore = match.isHome()
                ? match.getAwayGoalNumber()
                : match.getHomeGoalNumber();

        if (ourScore < opponentScore) {
            addFineToPlayers(match, playingPlayers, appTeam, "Prohra (pro hrající)");

            if (opponentScore - ourScore >= 5) {
                addFineToPlayers(match, playingPlayers, appTeam, "Prohra o 5 a více (pro hrající)");
            }

            addFineToPlayers(match, absentPlayers, appTeam, "Neúčast v zápase (prohra)");
        } else if (ourScore > opponentScore) {
            addFineToPlayers(match, absentPlayers, appTeam, "Neúčast v zápase (výhra)");
        } else {
            addFineToPlayers(match, absentPlayers, appTeam, "Neúčast v zápase (remíza)");
        }

        if (playingPlayers.size() <= 7) {
            addFineToPlayers(match, absentPlayers, appTeam, "Neúčast při 7. a méně lidech");
        }
    }

    private void addFineToPlayers(
            MatchEntity match,
            List<PlayerEntity> players,
            AppTeamEntity appTeam,
            String fineName
    ) {
        FineEntity fine = fineRepository
                .findByNameAndAppTeamId(fineName, appTeam.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nenalezena automatická pokuta: " + fineName
                ));
       receivedFineService.addMultipleFines(createReceivedFineForPlayer(match, players, fine), appTeam);
    }

    private ReceivedFineListDTO createReceivedFineForPlayer(
            MatchEntity match,
            List<PlayerEntity> players,
            FineEntity fine
    ) {
        ReceivedFineListDTO receivedFineListDTO = new ReceivedFineListDTO();
        receivedFineListDTO.setMatchId(match.getId());
        receivedFineListDTO.setPlayerIdList(playerService.convertPlayerListToPlayerIdList(players.stream().map(playerMapper::toDTO).toList()));
        List<ReceivedFineDTO> receivedFineDTOS = new ArrayList<>();
        ReceivedFineDTO receivedFineDTO = new ReceivedFineDTO();
        receivedFineDTO.setFine(fineMapper.toDTO(fine));
        receivedFineDTO.setFineNumber(1);
        receivedFineDTOS.add(receivedFineDTO);
        receivedFineListDTO.setFineList(receivedFineDTOS);
      return receivedFineListDTO;
    }

    private void deleteExistingAutomaticResultFines(Long matchId, Long appTeamId) {
        receivedFineRepository.deleteAutomaticResultFinesFromMatch(matchId, appTeamId);
    }
}