package com.jumbo.trus.service.fine;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.FineMapper;
import com.jumbo.trus.repository.FineRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.exceptions.NonEditableEntityException;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    private static final long TEAM_ID = 1L;

    @Mock private FineRepository fineRepository;
    @Mock private ReceivedFineRepository receivedFineRepository;
    @Mock private FineMapper fineMapper;
    @Mock private FineNotificationService fineNotificationService;
    @Mock private OutboxEventService outboxEventService;

    private FineService service;
    private AppTeamEntity appTeam;

    @BeforeEach
    void setUp() {
        service = new FineService(
                fineRepository,
                receivedFineRepository,
                fineMapper,
                fineNotificationService,
                outboxEventService
        );
        appTeam = new AppTeamEntity();
        appTeam.setId(TEAM_ID);
        lenient().when(receivedFineRepository.findMatchIdsByFineId(any())).thenReturn(Set.of());
        lenient().when(receivedFineRepository.findPlayerIdsByFineId(any())).thenReturn(Set.of());
        lenient().when(fineMapper.toDTO(any())).thenAnswer(invocation -> toDto(invocation.getArgument(0)));
        lenient().when(fineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(fineRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void amountChangeCreatesNewVersionAndKeepsCoreName() {
        FineEntity original = fine(10L, "Třetí poločas", FineCodes.THIRD_HALF, 100, false, false);
        when(fineRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(original));

        FineDTO result = service.editFine(
                10L,
                new FineDTO(10L, "Přejmenovaná pokuta", 150, false),
                appTeam
        );

        ArgumentCaptor<FineEntity> newVersion = ArgumentCaptor.forClass(FineEntity.class);
        verify(fineRepository).saveAndFlush(original);
        verify(fineRepository).save(newVersion.capture());

        assertTrue(original.isInactive());
        assertEquals(100, original.getAmount());
        assertEquals("Třetí poločas", original.getName());

        FineEntity saved = newVersion.getValue();
        assertNull(saved.getId());
        assertEquals(FineCodes.THIRD_HALF, saved.getCode());
        assertEquals("Třetí poločas", saved.getName());
        assertEquals(150, saved.getAmount());
        assertFalse(saved.isEditable());
        assertFalse(saved.isInactive());
        assertEquals(150, result.getAmount());
    }

    @Test
    void builtInArrivalFineCanChangeCategory() {
        FineEntity original = fine(11L, "Nepříchod", FineCodes.NO_SHOW, 100, false, false);
        when(fineRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(original));

        FineDTO update = new FineDTO(11L, "Nepříchod", 100, false);
        update.setCategory(FineCategories.MATCH);
        service.editFine(11L, update, appTeam);

        assertEquals(FineCategories.MATCH, original.getCategory());
    }

    @Test
    void thirdHalfCanBeMovedToPostMatchCategory() {
        FineEntity original = fine(13L, "Třetí poločas", FineCodes.THIRD_HALF, 100, false, false);
        when(fineRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(original));

        FineDTO update = new FineDTO(13L, original.getName(), 100, false);
        update.setCategory(FineCategories.POST_MATCH);
        service.editFine(13L, update, appTeam);

        assertEquals(FineCategories.POST_MATCH, original.getCategory());
    }

    @Test
    void systemFineCanBeMovedBackToAutomaticCategory() {
        FineEntity original = fine(14L, "Třetí poločas", FineCodes.THIRD_HALF, 100, false, false);
        when(fineRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(original));

        FineDTO update = new FineDTO(14L, original.getName(), 100, false);
        update.setCategory(FineCategories.AUTOMATIC);
        service.editFine(14L, update, appTeam);

        assertEquals(FineCategories.AUTOMATIC, original.getCategory());
    }

    @Test
    void lateNoticeCanChangeCategory() {
        FineEntity original = fine(12L, "Pozdní nahlášení pozdního příchodu", FineCodes.LATE_NOTICE, 100, false, false);
        when(fineRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(original));

        FineDTO update = new FineDTO(12L, original.getName(), 100, false);
        update.setCategory(FineCategories.PRE_MATCH);
        service.editFine(12L, update, appTeam);

        assertEquals(FineCategories.PRE_MATCH, original.getCategory());
    }

    @Test
    void customFineCanBeRenamedWithoutChangingHistoricalAmountVersion() {
        FineEntity original = fine(20L, "Původní", "CUSTOM_ABC", 50, true, false);
        when(fineRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(original));

        FineDTO result = service.editFine(
                20L,
                new FineDTO(20L, "Nový název", 50, true),
                appTeam
        );

        assertEquals("Nový název", result.getName());
        assertFalse(original.isInactive());
        verify(fineRepository, never()).saveAndFlush(any());
    }

    @Test
    void deletingCustomFineOnlyDeactivatesDefinitionAndKeepsHistory() {
        FineEntity custom = fine(20L, "Vlastní", "CUSTOM_ABC", 50, true, false);
        when(fineRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(custom));

        service.deleteFine(20L, appTeam);

        assertTrue(custom.isInactive());
        verify(fineRepository).save(custom);
        verify(fineRepository, never()).deleteById(anyLong());
    }

    @Test
    void coreFineCannotBeDeleted() {
        FineEntity core = fine(10L, "Svatba", FineCodes.WEDDING, 1000, false, false);
        when(fineRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(core));

        assertThrows(NonEditableEntityException.class, () -> service.deleteFine(10L, appTeam));

        verify(fineRepository, never()).save(any());
    }

    @Test
    void inactiveVersionCannotBeAssigned() {
        FineEntity historical = fine(10L, "Svatba", FineCodes.WEDDING, 1000, false, true);
        when(fineRepository.findById(10L)).thenReturn(Optional.of(historical));

        assertThrows(
                NonEditableEntityException.class,
                () -> service.getFineEntityForAssignment(10L, TEAM_ID)
        );
    }

    private FineEntity fine(
            long id,
            String name,
            String code,
            int amount,
            boolean editable,
            boolean inactive
    ) {
        FineEntity fine = new FineEntity();
        fine.setId(id);
        fine.setName(name);
        fine.setCode(code);
        fine.setAmount(amount);
        fine.setEditable(editable);
        fine.setInactive(inactive);
        fine.setAppTeam(appTeam);
        return fine;
    }

    private FineDTO toDto(FineEntity fine) {
        return new FineDTO(
                fine.getId() == null ? 0 : fine.getId(),
                fine.getName(),
                fine.getAmount(),
                fine.isInactive(),
                fine.getCode(),
                fine.isEditable()
        );
    }
}
