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
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;
    private final ReceivedFineRepository receivedFineRepository;
    private final FineMapper fineMapper;
    private final NonEditableFineHandler nonEditableFineHandler;
    private final FineNotificationService fineNotificationService;
    private final FineValidator fineValidator;
    private final OutboxEventService outboxEventService;

    /**
     * metoda uloží pokutu do db a založí notifikaci
     * @param fineDTO Pokuta z FE
     * @return Pokuta z DB
     */
    @Transactional
    public FineDTO addFine(FineDTO fineDTO, AppTeamEntity appTeam) {
        FineEntity entity = fineMapper.toEntity(fineDTO);
        entity.setAppTeam(appTeam);
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

    public FineDTO getFineByName(String name, long appTeamId) {
        return fineMapper.toDTO(fineRepository.findByNameAndAppTeamId(name, appTeamId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(name))));
    }

    public FineEntity getFineEntity(long fineId) {
        return fineRepository.findById(fineId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(fineId)));
    }

    /**
     * metoda upraví všechny pokuty. Pokud přijde parametr inactive,
     * tak se založí nová pokuta, která přepíše zbylé pokuty s původní částkou.
     * Pokud inactive=false, tak se pokuty přepíšou i s původní cenou
     * @param fineId id pokuty, kterou chceme změnit
     * @param fineDTO nové hodnoty pokuty
     * @return Pokutu nově uloženou v db
     * @throws NotFoundException pokud není pokuta dle id nalezena
     */
    @Transactional
    public FineDTO editFine(Long fineId, FineDTO fineDTO, AppTeamEntity appTeam) {
        fineValidator.validateExists(fineId);
        Set<Long> affectedMatchIds = receivedFineRepository.findMatchIdsByFineId(fineId);
        Set<Long> affectedPlayerIds = receivedFineRepository.findPlayerIdsByFineId(fineId);
        FineDTO updatedFine;
        if (fineDTO.isInactive()) {
            updatedFine = processInactiveFine(fineId, fineDTO, appTeam);
        } else if (nonEditableFineHandler.isNonEditableFine(fineId)) {
            updatedFine = processNonEditableFine(fineId, fineDTO);
        } else {
            updatedFine = processRegularFine(fineId, fineDTO, appTeam);
            fineNotificationService.notifyFineUpdated(fineDTO.getName(), fineDTO.getAmount());
        }
        outboxEventService.createEvent(OutboxEventType.FINE_UPDATED, OutboxAggregateType.FINE, fineId, OutboxEventPayloadFactory.fineUpdated(affectedMatchIds, affectedPlayerIds));
        return updatedFine;
    }

    @Transactional
    public void deleteFine(Long fineId) {
        fineValidator.validateNotNonEditable(fineId);
        FineEntity fineEntity = fineRepository.findById(fineId).orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
        Set<Long> affectedMatchIds = receivedFineRepository.findMatchIdsByFineId(fineId);
        Set<Long> affectedPlayerIds = receivedFineRepository.findPlayerIdsByFineId(fineId);
        fineNotificationService.notifyFineDeleted(fineEntity.getName(), fineEntity.getAmount());
        receivedFineRepository.deleteByFineId(fineId);
        fineRepository.deleteById(fineId);
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

    private FineDTO processInactiveFine(Long fineId, FineDTO fineDTO, AppTeamEntity appTeam) {
        FineEntity existingFine = fineRepository.findById(fineId).orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
        if (nonEditableFineHandler.isNonEditableFine(fineId)) {
            FineEntity savedFine = duplicateAndDeactivateFine(existingFine);
            receivedFineRepository.updateByFineId(fineId, savedFine.getId());
            fineDTO.setInactive(false);
            return processNonEditableFine(fineId, fineDTO);
        }
        existingFine.setInactive(true);
        fineRepository.save(existingFine);
        FineEntity newFine = fineMapper.toEntity(fineDTO);
        newFine.setId(null);
        newFine.setInactive(false);
        newFine.setAppTeam(appTeam);
        return fineMapper.toDTO(fineRepository.save(newFine));
    }

    private FineEntity duplicateAndDeactivateFine(FineEntity fineEntity) {
        return nonEditableFineHandler.duplicateAndDeactivate(fineEntity);
    }

    private FineDTO processNonEditableFine(Long fineId, FineDTO fineDTO) {
        return nonEditableFineHandler.process(fineId, fineDTO);
    }

    private FineDTO processRegularFine(Long fineId, FineDTO fineDTO, AppTeamEntity appTeam) {
        FineEntity entity = fineMapper.toEntity(fineDTO);
        entity.setId(fineId);
        entity.setAppTeam(appTeam);
        return fineMapper.toDTO(fineRepository.save(entity));
    }
}
