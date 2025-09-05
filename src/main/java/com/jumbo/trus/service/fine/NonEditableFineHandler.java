package com.jumbo.trus.service.fine;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.repository.FineRepository;
import com.jumbo.trus.mapper.FineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.webjars.NotFoundException;

@Component
@RequiredArgsConstructor
public class NonEditableFineHandler {

    private final FineRepository fineRepository;
    private final FineMapper fineMapper;
    private final FineNotificationService fineNotificationService;

    public FineDTO process(Long fineId, FineDTO fineDTO) {
        FineEntity fineEntity = fineRepository.findById(fineId)
            .orElseThrow(() -> new NotFoundException("Pokuta nenalezena v db"));
        fineEntity.setAmount(fineDTO.getAmount());
        if (fineId == Config.GOAL_FINE_ID) {
            fineEntity.setName("Gól");
        }
        fineRepository.save(fineEntity);
        fineNotificationService.notifyFineUpdated(fineDTO.getName(), fineDTO.getAmount());
        return fineMapper.toDTO(fineEntity);
    }

    public FineEntity duplicateAndDeactivate(FineEntity fineEntity) {
        FineEntity newFine = new FineEntity();
        newFine.setEditable(false);
        newFine.setInactive(true);
        newFine.setName(fineEntity.getName());
        newFine.setAmount(fineEntity.getAmount());
        return fineRepository.save(newFine);
    }

    public boolean isNonEditableFine(Long fineId) {
        return fineId == Config.GOAL_FINE_ID || fineId == Config.HATTRICK_FINE_ID;
    }
}
