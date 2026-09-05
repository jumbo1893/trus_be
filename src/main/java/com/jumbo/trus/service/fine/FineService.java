package com.jumbo.trus.service.fine;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.mapper.FineMapper;
import com.jumbo.trus.repository.FineRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.service.exceptions.NonEditableEntityException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;
    private final ReceivedFineRepository receivedFineRepository;
    private final FineMapper fineMapper;
    private final FineNotificationService fineNotificationService;
    private final OutboxEventService outboxEventService;

    /**
     * metoda uloží pokutu do db a založí notifikaci
     * @param fineDTO Pokuta z FE
     * @return Pokuta z DB
     */
    @Transactional
    public FineDTO addFine(FineDTO fineDTO, AppTeamEntity appTeam) {
        FineEntity entity = fineMapper.toEntity(fineDTO);
        entity.setId(null);
        entity.setAppTeam(appTeam);
        entity.setCode(FineCodes.CUSTOM_PREFIX + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        entity.setEditable(true);
        entity.setInactive(false);
        FineEntity savedEntity = fineRepository.save(entity);
        fineNotificationService.notifyFineAdded(fineDTO.getName(), fineDTO.getAmount());
        outboxEventService.createEvent(OutboxEventType.FINE_CREATED, OutboxAggregateType.FINE, savedEntity.getId(), OutboxEventPayloadFactory.fineCreated(Set.of()));
        return fineMapper.toDTO(savedEntity);
    }

    /**
     *
     * @param limit limit počtu výsledků
     * @return všechny pokuty omezené limitem
     */
    public List<FineDTO> getAll(int limit, long appTeamId){
        return fineRepository.getAllActive(limit, appTeamId).stream()
                .map(fineMapper::toDTO)
                .collect(Collectors.toList());
    }

    public FineDTO getFine(long fineId) {
        return fineMapper.toDTO(getFineEntity(fineId));
    }

    public FineDTO getFineByCode(String code, long appTeamId) {
        return fineMapper.toDTO(getActiveFineEntityByCode(code, appTeamId));
    }

    public List<FineDTO> getStatisticsOptions(Long appTeamId) {
        return fineRepository.findAllByAppTeamIdOrderByNameAsc(appTeamId).stream()
                .map(fineMapper::toDTO).toList();
    }

    public FineEntity getActiveFineEntityByCode(String code, long appTeamId) {
        return fineRepository.findFirstByCodeAndAppTeamIdAndInactiveFalse(code, appTeamId)
                .orElseThrow(() -> new EntityNotFoundException(code));
    }

    public FineEntity getFineEntity(long fineId) {
        return fineRepository.findById(fineId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(fineId)));
    }

    public FineEntity getFineEntityForAssignment(long fineId, long appTeamId) {
        FineEntity fine = getFineEntity(fineId);
        validateBelongsToTeam(fine, appTeamId);
        if (fine.isInactive()) {
            throw new NonEditableEntityException("Neaktivní historickou verzi pokuty nelze udělit ani upravit");
        }
        return fine;
    }

    /**
     * Updates an active fine definition. Every amount change creates a new
     * active version; already awarded fines keep referencing the old version.
     * Core fine names are immutable, while custom fine names may be changed.
     */
    @Transactional
    public FineDTO editFine(Long fineId, FineDTO fineDTO, AppTeamEntity appTeam) {
        FineEntity existingFine = getFineForUpdate(fineId, appTeam.getId());
        if (existingFine.isInactive()) {
            throw new NonEditableEntityException("Neaktivní historickou verzi pokuty nelze upravit");
        }
        Set<Long> affectedMatchIds = receivedFineRepository.findMatchIdsByFineId(fineId);
        Set<Long> affectedPlayerIds = receivedFineRepository.findPlayerIdsByFineId(fineId);
        String updatedName = existingFine.isEditable() ? fineDTO.getName() : existingFine.getName();
        FineEntity savedFine;

        if (existingFine.getAmount() != fineDTO.getAmount()) {
            savedFine = createNewAmountVersion(existingFine, updatedName, fineDTO.getAmount());
        } else {
            existingFine.setName(updatedName);
            savedFine = fineRepository.save(existingFine);
        }
        fineNotificationService.notifyFineUpdated(savedFine.getName(), savedFine.getAmount());
        outboxEventService.createEvent(OutboxEventType.FINE_UPDATED, OutboxAggregateType.FINE, fineId, OutboxEventPayloadFactory.fineUpdated(affectedMatchIds, affectedPlayerIds));
        return fineMapper.toDTO(savedFine);
    }

    @Transactional
    public void deleteFine(Long fineId, AppTeamEntity appTeam) {
        FineEntity fineEntity = getFineForUpdate(fineId, appTeam.getId());
        if (!fineEntity.isEditable()) {
            throw new NonEditableEntityException("Core pokutu nelze smazat ani přejmenovat");
        }
        if (fineEntity.isInactive()) {
            throw new NonEditableEntityException("Neaktivní historickou verzi pokuty nelze smazat");
        }
        Set<Long> affectedMatchIds = receivedFineRepository.findMatchIdsByFineId(fineId);
        Set<Long> affectedPlayerIds = receivedFineRepository.findPlayerIdsByFineId(fineId);
        fineNotificationService.notifyFineDeleted(fineEntity.getName(), fineEntity.getAmount());
        fineEntity.setInactive(true);
        fineRepository.save(fineEntity);
        outboxEventService.createEvent(OutboxEventType.FINE_DELETED, OutboxAggregateType.FINE, fineId, OutboxEventPayloadFactory.fineDeleted(affectedMatchIds, affectedPlayerIds));

    }

    /**
     *
     * @param excludedFineIds id pokut
     * @return vrátí všechny ostatní pokuty krom těchto pokut, pokud nejsou neaktivní
     */
    public List<FineDTO> getFinesExcluding(List<Long> excludedFineIds, long appTeamId){
        return fineRepository.getAllOtherFines(excludedFineIds, appTeamId).stream().map(fineMapper::toDTO).collect(Collectors.toList());

    }

    private FineEntity createNewAmountVersion(FineEntity existingFine, String name, int amount) {
        existingFine.setInactive(true);
        fineRepository.saveAndFlush(existingFine);

        FineEntity newFine = new FineEntity();
        newFine.setName(name);
        newFine.setCode(existingFine.getCode());
        newFine.setAmount(amount);
        newFine.setEditable(existingFine.isEditable());
        newFine.setInactive(false);
        newFine.setAppTeam(existingFine.getAppTeam());
        return fineRepository.save(newFine);
    }

    private FineEntity getFineForUpdate(Long fineId, long appTeamId) {
        FineEntity fine = fineRepository.findByIdForUpdate(fineId)
                .orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
        validateBelongsToTeam(fine, appTeamId);
        return fine;
    }

    private void validateBelongsToTeam(FineEntity fine, long appTeamId) {
        if (fine.getAppTeam() == null || !fine.getAppTeam().getId().equals(appTeamId)) {
            throw new EntityNotFoundException(String.valueOf(fine.getId()));
        }
    }
}
