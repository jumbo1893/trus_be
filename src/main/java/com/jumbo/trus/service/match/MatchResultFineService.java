package com.jumbo.trus.service.match;

import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.FineRepository;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchResultFineService {

    private final ReceivedFineRepository receivedFineRepository;
    private final FineRepository fineRepository;
    private final PlayerRepository playerRepository;

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

        List<ReceivedFineEntity> fines = players.stream()
                .map(player -> createReceivedFine(match, player, fine, appTeam))
                .toList();

        receivedFineRepository.saveAll(fines);
    }

    private ReceivedFineEntity createReceivedFine(
            MatchEntity match,
            PlayerEntity player,
            FineEntity fine,
            AppTeamEntity appTeam
    ) {
        ReceivedFineEntity entity = new ReceivedFineEntity();
        entity.setMatch(match);
        entity.setPlayer(player);
        entity.setFine(fine);
        entity.setFineNumber(1);
        entity.setAppTeam(appTeam);
        return entity;
    }

    private void deleteExistingAutomaticResultFines(Long matchId, Long appTeamId) {
        receivedFineRepository.deleteAutomaticResultFinesFromMatch(matchId, appTeamId);
    }
}