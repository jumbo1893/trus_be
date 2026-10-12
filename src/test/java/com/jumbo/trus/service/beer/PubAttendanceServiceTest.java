package com.jumbo.trus.service.beer;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.beer.PubAttendanceDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.fine.FineCodes;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PubAttendanceServiceTest {
    @Mock MatchRepository matchRepository;
    @Mock ReceivedFineRepository receivedFineRepository;
    @Mock ReceivedFineService receivedFineService;
    @Mock FineService fineService;
    @Mock OutboxEventService outboxEventService;
    @InjectMocks PubAttendanceService service;

    private AppTeamEntity team;
    private MatchEntity match;

    private PlayerEntity player(long id, boolean fan) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setFan(fan);
        return player;
    }

    @BeforeEach
    void setup() {
        team = new AppTeamEntity();
        team.setId(7L);
        match = new MatchEntity();
        match.setId(10L);
        match.setPlayerList(List.of(player(1, false), player(2, false), player(3, true)));
        when(matchRepository.findForPubUpdate(10L, 7L)).thenReturn(Optional.of(match));
    }

    @Test
    void finesOnlyAbsentPlayersAndRemembersLeftGuests() {
        when(receivedFineRepository.findForMatchPlayerCode(10L, 2L, FineCodes.THIRD_HALF, 7L))
                .thenReturn(List.of());
        when(fineService.getFineByCode(FineCodes.THIRD_HALF, 7L))
                .thenReturn(new FineDTO(4, "Třetí poločas", 50, false));
        ReceivedFineDTO created = new ReceivedFineDTO();
        created.setId(99L);
        created.setFine(new FineDTO(4, "Třetí poločas", 50, false));
        when(receivedFineService.addFine(any(), eq(team))).thenReturn(created);

        PubAttendanceDTO result = service.save(
                new PubAttendanceDTO(10L, true, Set.of(1L), Set.of(1L)), team);

        assertTrue(result.isConfigured());
        assertEquals(Set.of(1L), result.getGuestIds());
        assertEquals(Set.of(1L), result.getLeftIds());
        assertEquals(Set.of(99L), match.getPubAutoFineIds());
        verify(receivedFineService).addFine(argThat(fine ->
                fine.getPlayerId() == 2L && fine.getFineNumber() == 1), eq(team));
        verify(receivedFineService, times(1)).addFine(any(), eq(team));
        verify(outboxEventService).createEvent(eq(OutboxEventType.RECEIVED_FINE_CHANGED),
                any(), isNull(), any());
    }

    @Test
    void existingThirdHalfFineIsNotDuplicated() {
        when(receivedFineRepository.findForMatchPlayerCode(10L, 2L, FineCodes.THIRD_HALF, 7L))
                .thenReturn(List.of(new ReceivedFineEntity()));

        service.save(new PubAttendanceDTO(10L, true, Set.of(1L), Set.of()), team);

        verifyNoInteractions(receivedFineService, fineService);
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void rejectsLeftPlayerWhoWasNeverGuest() {
        assertThrows(ResponseStatusException.class, () ->
                service.save(new PubAttendanceDTO(10L, true, Set.of(1L), Set.of(2L)), team));
        verifyNoInteractions(receivedFineService);
    }

    @Test
    void correctionRemovesOnlyAutomaticallyCreatedFine() {
        match.getPubAutoFineIds().add(99L);
        ReceivedFineEntity automatic = new ReceivedFineEntity();
        automatic.setId(99L);
        automatic.setPlayer(match.getPlayerList().get(1));
        FineEntity fine = new FineEntity();
        fine.setId(4L);
        automatic.setFine(fine);
        when(receivedFineRepository.findAllById(Set.of(99L)))
                .thenReturn(List.of(automatic));

        service.save(new PubAttendanceDTO(10L, true, Set.of(1L, 2L), Set.of(1L)), team);

        verify(receivedFineRepository).delete(automatic);
        assertTrue(match.getPubAutoFineIds().isEmpty());
        verifyNoInteractions(receivedFineService);
    }
}
