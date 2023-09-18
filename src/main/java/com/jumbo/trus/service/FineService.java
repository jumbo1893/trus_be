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

    public FineDTO addFine(FineDTO fineDTO) {
        FineEntity entity = fineMapper.toEntity(fineDTO);
        FineEntity savedEntity = fineRepository.save(entity);
        notificationService.addNotification("Přidána pokuta " + fineDTO.getName(), "ve výši " + fineDTO.getAmount() + " Kč");
        return fineMapper.toDTO(savedEntity);
    }

    public List<FineDTO> getAll(int limit){
        List<FineEntity> fineEntities = fineRepository.getAll(limit);
        List<FineDTO> result = new ArrayList<>();
        for(FineEntity e : fineEntities){
            result.add(fineMapper.toDTO(e));
        }
        return result;
    }

    public FineDTO editFine(Long fineId, FineDTO fineDTO) throws NotFoundException {
        if (!fineRepository.existsById(fineId)) {
            throw new NotFoundException("Pokuta s id " + fineId + " nenalezena v db");
        }
        if(fineId == Config.GOAL_FINE_ID || fineId == Config.HATTRICK_FINE_ID) {
            FineEntity fineEntity = fineRepository.findById(fineId).orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
            fineEntity.setAmount(fineDTO.getAmount());
            if (fineId == Config.GOAL_FINE_ID) {
                fineEntity.setName("Gól");
            }
            fineRepository.save(fineEntity);
            return fineMapper.toDTO(fineEntity);
        }

        FineEntity entity = fineMapper.toEntity(fineDTO);
        entity.setId(fineId);
        FineEntity savedEntity = fineRepository.save(entity);
        notificationService.addNotification("Upravena pokuta " + fineDTO.getName(), "ve výši " + fineDTO.getAmount() + " Kč");
        return fineMapper.toDTO(savedEntity);
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

    public List<FineDTO> getAllOtherFines (List<Long> otherFines){
        return fineRepository.getAllOtherFines(otherFines).stream().map(fineMapper::toDTO).collect(Collectors.toList());

    }
}
