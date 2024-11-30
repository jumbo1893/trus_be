package com.jumbo.trus.service.fine;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.entity.repository.FineRepository;
import com.jumbo.trus.service.exceptions.NonEditableEntityException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.webjars.NotFoundException;

@Component
@RequiredArgsConstructor
public class FineValidator {

    private final FineRepository fineRepository;

    public void validateExists(Long fineId) {
        if (!fineRepository.existsById(fineId)) {
            throw new NotFoundException("Pokuta s id " + fineId + " nenalezena v db");
        }
    }

    public void validateNotNonEditable(Long fineId) {
        if (fineId == Config.GOAL_FINE_ID || fineId == Config.HATTRICK_FINE_ID) {
            throw new NonEditableEntityException("Tuto pokutu nelze smazat nebo upravit");
        }
    }
}
