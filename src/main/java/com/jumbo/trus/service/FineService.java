package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.entity.repository.ReceivedFineRepository;
import com.jumbo.trus.mapper.FineMapper;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.repository.FineRepository;
import com.jumbo.trus.service.exceptions.NonEditableEntityException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FineService {

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private FineMapper fineMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * metoda uloží pokutu do db a založí notifikaci
     * @param fineDTO Pokuta z FE
     * @return Pokuta z DB
     */
    public FineDTO addFine(FineDTO fineDTO) {
        FineEntity entity = fineMapper.toEntity(fineDTO);
        FineEntity savedEntity = fineRepository.save(entity);
        notificationService.addNotification("Přidána pokuta " + fineDTO.getName(), "ve výši " + fineDTO.getAmount() + " Kč");
        return fineMapper.toDTO(savedEntity);
    }

    /**
     *
     * @param limit limit počtu výsledků
     * @return všechny pokuty omezené limitem
     */
    public List<FineDTO> getAll(int limit){
        List<FineEntity> fineEntities = fineRepository.getAllActive(limit);
        List<FineDTO> result = new ArrayList<>();
        for(FineEntity e : fineEntities){
            result.add(fineMapper.toDTO(e));
        }
        return result;
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
    public FineDTO editFine(Long fineId, FineDTO fineDTO) throws NotFoundException {
        if (!fineRepository.existsById(fineId)) {
            throw new NotFoundException("Pokuta s id " + fineId + " nenalezena v db");
        }
        if (fineDTO.isInactive()) {
            if(fineId == Config.GOAL_FINE_ID || fineId == Config.HATTRICK_FINE_ID) {
                FineEntity nonEditableEntity = fineRepository.findById(fineId).orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
                FineEntity fineEntity = new FineEntity();
                fineEntity.setEditable(false);
                fineEntity.setInactive(true);
                fineEntity.setName(nonEditableEntity.getName());
                fineEntity.setAmount(nonEditableEntity.getAmount());
                FineEntity savedEntity = fineRepository.save(fineEntity);
                receivedFineRepository.updateByFineId(fineId, savedEntity.getId());
                fineDTO.setInactive(false);
                return editNonEditableFines(fineId, fineDTO);
            }
            FineEntity inactiveEntity = fineRepository.findById(fineId).orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
            inactiveEntity.setInactive(true);
            fineRepository.save(inactiveEntity);
            FineEntity entity = fineMapper.toEntity(fineDTO);
            entity.setId(null);
            entity.setInactive(false);
            notificationService.addNotification("Upravena pokuta " + fineDTO.getName(), "ve výši " + fineDTO.getAmount() + " Kč");
            FineEntity savedEntity = fineRepository.save(entity);
            return fineMapper.toDTO(savedEntity);
        }
        if(fineId == Config.GOAL_FINE_ID || fineId == Config.HATTRICK_FINE_ID) {
            return editNonEditableFines(fineId, fineDTO);
        }
        FineEntity entity = fineMapper.toEntity(fineDTO);
        entity.setId(fineId);
        FineEntity savedEntity = fineRepository.save(entity);
        notificationService.addNotification("Upravena pokuta " + fineDTO.getName(), "ve výši " + fineDTO.getAmount() + " Kč");
        return fineMapper.toDTO(savedEntity);
    }

    /**
     * metoda přepisuje needitovatelné pokuty, tedy pokuta gól a hattrick, jejichž id je již jinde v kódu.
     * @param fineId id pokuty, kterou chceme změnit
     * @param fineDTO nové hodnoty pokuty
     * @return Pokutu nově uloženou v db
     */
    private FineDTO editNonEditableFines(Long fineId, FineDTO fineDTO) {
        FineEntity fineEntity = fineRepository.findById(fineId).orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
        fineEntity.setAmount(fineDTO.getAmount());
        if (fineId == Config.GOAL_FINE_ID) {
            fineEntity.setName("Gól");
        }
        fineRepository.save(fineEntity);
        notificationService.addNotification("Upravena pokuta " + fineDTO.getName(), "ve výši " + fineDTO.getAmount() + " Kč");
        return fineMapper.toDTO(fineEntity);
    }

    @Transactional
    public void deleteFine(Long fineId) {
        if(fineId == Config.GOAL_FINE_ID || fineId == Config.HATTRICK_FINE_ID) {
            throw new NonEditableEntityException("Tuto pokutu nelze smazat");
        }
        else {
            FineEntity fineEntity = fineRepository.getReferenceById(fineId);
            notificationService.addNotification("Smazána pokuta " + fineEntity.getName(), "ve výši " + fineEntity.getAmount() + " Kč");
            receivedFineRepository.deleteByFineId(fineId);
            fineRepository.deleteById(fineId);
        }
    }

    /**
     *
     * @param otherFines id pokut
     * @return vrátí všechny ostatní pokuty krom těchto pokut, pokud nejsou neaktivní
     */
    public List<FineDTO> getAllOtherFines (List<Long> otherFines){
        return fineRepository.getAllOtherFines(otherFines).stream().map(fineMapper::toDTO).collect(Collectors.toList());

    }
}
