package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.FineMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.fine.FineCodes;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.receivedFine.ReceivedFineUpdater;
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

    private static final List<String> AUTOMATIC_RESULT_FINE_CODES = List.of(
            FineCodes.LOSS_BY_FIVE_PLAYING,
            FineCodes.LOSS_PLAYING,
            FineCodes.ABSENT_WHEN_SEVEN_OR_FEWER,
            FineCodes.ABSENT_WIN,
            FineCodes.ABSENT_DRAW,
            FineCodes.ABSENT_LOSS
    );

    private final ReceivedFineRepository receivedFineRepository;
    private final FineService fineService;
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
            addFineToPlayers(match, playingPlayers, appTeam, FineCodes.LOSS_PLAYING);

            if (opponentScore - ourScore >= 5) {
                addFineToPlayers(match, playingPlayers, appTeam, FineCodes.LOSS_BY_FIVE_PLAYING);
            }

            addFineToPlayers(match, absentPlayers, appTeam, FineCodes.ABSENT_LOSS);
        } else if (ourScore > opponentScore) {
            addFineToPlayers(match, absentPlayers, appTeam, FineCodes.ABSENT_WIN);
        } else {
            addFineToPlayers(match, absentPlayers, appTeam, FineCodes.ABSENT_DRAW);
        }

        if (playingPlayers.size() <= 7) {
            addFineToPlayers(match, absentPlayers, appTeam, FineCodes.ABSENT_WHEN_SEVEN_OR_FEWER);
        }
    }

    private void addFineToPlayers(
            MatchEntity match,
            List<PlayerEntity> players,
            AppTeamEntity appTeam,
            String fineCode
    ) {
        FineEntity fine = fineService.getActiveFineEntityByCode(fineCode, appTeam.getId());
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
        receivedFineRepository.deleteAutomaticResultFinesFromMatch(
                matchId, appTeamId, AUTOMATIC_RESULT_FINE_CODES);
    }
}
