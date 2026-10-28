package com.jumbo.trus.service.receivedFine;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.GoalEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.ReceivedFineMapper;
import com.jumbo.trus.repository.GoalRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.fine.FineCodes;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.notification.push.maker.FineNotificationMaker;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.service.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ReceivedFineUpdaterCleanSheetTest {
    private final ReceivedFineRepository fines = mock(ReceivedFineRepository.class);
    private final GoalRepository goals = mock(GoalRepository.class);
    private final ReceivedFineMapper mapper = mock(ReceivedFineMapper.class);
    private final PlayerService players = mock(PlayerService.class);
    private final FineService fineService = mock(FineService.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final ReceivedFineUpdater updater = new ReceivedFineUpdater(
            fines, goals, mapper, players, fineService, mock(NotificationService.class),
            mock(FineNotificationMaker.class), matches, mock(OutboxEventService.class));
    private final AppTeamEntity team = mock(AppTeamEntity.class);

    @Test
    void awardsActivePlayersAndInactiveParticipantsExceptGoalkeepers() {
        when(team.getId()).thenReturn(7L);
        MatchEntity match = match(2, 0, List.of(player(1, true, false),
                player(4, false, false), player(5, false, true)));
        when(matches.findById(10L)).thenReturn(Optional.of(match));
        when(fines.findByMatchIdAndAppTeamIdAndAutomaticSource(10L, 7L, "CLEAN_SHEET"))
                .thenReturn(List.of());
        when(goals.findByMatchIdAndGoalkeepingMinutesGreaterThan(10L, 0))
                .thenReturn(List.of(goalFor(player(1, true, false))));
        when(players.getAllActive(true, 7L)).thenReturn(List.of(dto(1), dto(2), dto(3)));
        FineDTO fine = new FineDTO();
        fine.setId(20L);
        when(fineService.getFineByCode(FineCodes.ZERO_IN_MATCH, 7L)).thenReturn(fine);
        when(fineService.getFineEntityForAssignment(20L, 7L)).thenReturn(mock(FineEntity.class));
        when(mapper.toEntity(any())).thenAnswer(invocation -> new ReceivedFineEntity());
        when(players.getPlayerEntity(anyLong())).thenAnswer(invocation ->
                player(invocation.getArgument(0), true, false));
        AtomicLong id = new AtomicLong();
        when(fines.save(any())).thenAnswer(invocation -> {
            ReceivedFineEntity entity = invocation.getArgument(0);
            entity.setId(id.incrementAndGet());
            return entity;
        });

        updater.syncCleanSheetFines(10L, team);

        ArgumentCaptor<ReceivedFineEntity> saved = ArgumentCaptor.forClass(ReceivedFineEntity.class);
        verify(fines, times(3)).save(saved.capture());
        assertThat(saved.getAllValues().stream().map(value -> value.getPlayer().getId())
                .collect(Collectors.toSet())).isEqualTo(Set.of(2L, 3L, 4L));
        assertThat(saved.getAllValues()).allSatisfy(value ->
                assertThat(value.getAutomaticSource()).isEqualTo("CLEAN_SHEET"));
    }

    @Test
    void removesOnlyAutomaticFineWhenScoreIsMissingOrGoalIsConceded() {
        when(team.getId()).thenReturn(7L);
        ReceivedFineEntity automatic = new ReceivedFineEntity();
        automatic.setId(30L);
        automatic.setPlayer(player(2, true, false));
        FineEntity fine = new FineEntity();
        fine.setId(20L);
        automatic.setFine(fine);
        when(fines.findByMatchIdAndAppTeamIdAndAutomaticSource(10L, 7L, "CLEAN_SHEET"))
                .thenReturn(List.of(automatic));
        when(goals.findByMatchIdAndGoalkeepingMinutesGreaterThan(10L, 0))
                .thenReturn(List.of(goalFor(player(1, true, false))));
        MatchEntity match = match(null, 0, List.of());
        when(matches.findById(10L)).thenReturn(Optional.of(match));

        updater.syncCleanSheetFines(10L, team);
        verify(fines).delete(automatic);
        verify(fines, never()).save(any());

        match.setHomeGoalNumber(2);
        match.setAwayGoalNumber(1);
        updater.syncCleanSheetFines(10L, team);
        verify(fines, times(2)).delete(automatic);
        verify(fines, never()).save(any());
    }

    @Test
    void recalculatesRecipientsWhenGoalkeeperChanges() {
        when(team.getId()).thenReturn(7L);
        MatchEntity match = match(2, 0, List.of(player(1, true, false), player(2, true, false)));
        when(matches.findById(10L)).thenReturn(Optional.of(match));
        when(goals.findByMatchIdAndGoalkeepingMinutesGreaterThan(10L, 0))
                .thenReturn(List.of(goalFor(player(2, true, false))));
        when(players.getAllActive(true, 7L)).thenReturn(List.of(dto(1), dto(2)));
        ReceivedFineEntity oldFine = new ReceivedFineEntity();
        oldFine.setId(30L);
        oldFine.setPlayer(player(2, true, false));
        FineEntity fineEntity = new FineEntity();
        fineEntity.setId(20L);
        oldFine.setFine(fineEntity);
        when(fines.findByMatchIdAndAppTeamIdAndAutomaticSource(10L, 7L, "CLEAN_SHEET"))
                .thenReturn(List.of(oldFine));
        FineDTO fine = new FineDTO();
        fine.setId(20L);
        when(fineService.getFineByCode(FineCodes.ZERO_IN_MATCH, 7L)).thenReturn(fine);
        when(fineService.getFineEntityForAssignment(20L, 7L)).thenReturn(fineEntity);
        when(mapper.toEntity(any())).thenReturn(new ReceivedFineEntity());
        when(players.getPlayerEntity(1L)).thenReturn(player(1, true, false));
        when(fines.save(any())).thenAnswer(invocation -> {
            ReceivedFineEntity entity = invocation.getArgument(0);
            entity.setId(31L);
            return entity;
        });

        updater.syncCleanSheetFines(10L, team);

        verify(fines).delete(oldFine);
        ArgumentCaptor<ReceivedFineEntity> saved = ArgumentCaptor.forClass(ReceivedFineEntity.class);
        verify(fines).save(saved.capture());
        assertThat(saved.getValue().getPlayer().getId()).isEqualTo(1L);
        assertThat(saved.getValue().getAutomaticSource()).isEqualTo("CLEAN_SHEET");
    }

    private MatchEntity match(Integer home, Integer away, List<PlayerEntity> participants) {
        MatchEntity result = new MatchEntity();
        result.setId(10L);
        result.setHome(true);
        result.setHomeGoalNumber(home);
        result.setAwayGoalNumber(away);
        result.setPlayerList(participants);
        return result;
    }

    private PlayerEntity player(long id, boolean active, boolean fan) {
        PlayerEntity result = new PlayerEntity();
        result.setId(id);
        result.setActive(active);
        result.setFan(fan);
        return result;
    }

    private PlayerDTO dto(long id) {
        PlayerDTO result = new PlayerDTO();
        result.setId(id);
        return result;
    }

    private GoalEntity goalFor(PlayerEntity player) {
        GoalEntity result = new GoalEntity();
        result.setPlayer(player);
        result.setGoalkeepingMinutes(60);
        return result;
    }
}
