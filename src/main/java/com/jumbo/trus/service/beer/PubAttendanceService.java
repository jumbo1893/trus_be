package com.jumbo.trus.service.beer;

import com.jumbo.trus.dto.beer.PubAttendanceDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.fine.FineCodes;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PubAttendanceService {
    private final MatchRepository matchRepository;
    private final ReceivedFineRepository receivedFineRepository;
    private final ReceivedFineService receivedFineService;
    private final FineService fineService;
    private final OutboxEventService outboxEventService;

    @Transactional
    public PubAttendanceDTO get(Long matchId, AppTeamEntity appTeam) {
        MatchEntity match = matchRepository.findByIdAndAppTeamId(matchId, appTeam.getId())
                .orElseThrow(() -> new EntityNotFoundException("Zápas nebyl nalezen"));
        return toDTO(match);
    }

    @Transactional
    public PubAttendanceDTO save(PubAttendanceDTO request, AppTeamEntity appTeam) {
        if (request.getMatchId() == null || request.getGuestIds() == null ||
                request.getLeftIds() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chybí údaje o hospodě");
        }
        MatchEntity match = matchRepository.findForPubUpdate(request.getMatchId(), appTeam.getId())
                .orElseThrow(() -> new EntityNotFoundException("Zápas nebyl nalezen"));
        Set<Long> eligible = match.getPlayerList().stream()
                .map(PlayerEntity::getId).collect(Collectors.toSet());
        if (!eligible.containsAll(request.getGuestIds()) ||
                !request.getGuestIds().containsAll(request.getLeftIds())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neplatní účastníci hospody");
        }

        Set<Long> guests = new HashSet<>(request.getGuestIds());
        Set<Long> autoFineIds = match.getPubAutoFineIds();
        Set<Long> affectedPlayers = new HashSet<>();
        Set<Long> changedFineIds = new HashSet<>();
        Set<Long> receivedFineIds = new HashSet<>();
        for (ReceivedFineEntity autoFine : receivedFineRepository.findAllById(autoFineIds)) {
            if (guests.contains(autoFine.getPlayer().getId())) {
                affectedPlayers.add(autoFine.getPlayer().getId());
                changedFineIds.add(autoFine.getFine().getId());
                receivedFineIds.add(autoFine.getId());
                receivedFineRepository.delete(autoFine);
                autoFineIds.remove(autoFine.getId());
            }
        }

        for (PlayerEntity player : match.getPlayerList()) {
            if (player.isFan() || guests.contains(player.getId())) continue;
            if (!receivedFineRepository.findForMatchPlayerCode(
                    match.getId(), player.getId(), FineCodes.THIRD_HALF, appTeam.getId()).isEmpty()) {
                continue;
            }
            ReceivedFineDTO created = receivedFineService.addFine(new ReceivedFineDTO(
                    1,
                    fineService.getFineByCode(FineCodes.THIRD_HALF, appTeam.getId()),
                    player.getId(),
                    match.getId()
            ), appTeam);
            autoFineIds.add(created.getId());
            affectedPlayers.add(player.getId());
            changedFineIds.add(created.getFine().getId());
            receivedFineIds.add(created.getId());
        }

        match.setPubConfigured(true);
        match.getPubGuestIds().clear();
        match.getPubGuestIds().addAll(guests);
        match.getPubLeftIds().clear();
        match.getPubLeftIds().addAll(request.getLeftIds());
        if (!affectedPlayers.isEmpty()) {
            outboxEventService.createEvent(OutboxEventType.RECEIVED_FINE_CHANGED,
                    OutboxAggregateType.RECEIVED_FINE, null,
                    OutboxEventPayloadFactory.receivedFinesChanged(
                            match.getId(), matchRepository.findSeasonIdByMatchId(match.getId()),
                            affectedPlayers, receivedFineIds, changedFineIds));
        }
        return toDTO(match);
    }

    private PubAttendanceDTO toDTO(MatchEntity match) {
        return new PubAttendanceDTO(
                match.getId(),
                Boolean.TRUE.equals(match.getPubConfigured()),
                Set.copyOf(match.getPubGuestIds()),
                Set.copyOf(match.getPubLeftIds())
        );
    }
}
