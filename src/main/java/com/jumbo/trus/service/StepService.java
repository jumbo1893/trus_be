package com.jumbo.trus.service;

import com.jumbo.trus.dto.StepDTO;
import com.jumbo.trus.dto.StepUpdateDTO;
import com.jumbo.trus.entity.StepUpdateEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.filter.StepFilter;
import com.jumbo.trus.repository.StepUpdateRepository;
import com.jumbo.trus.repository.specification.StepSpecification;
import com.jumbo.trus.mapper.StepUpdateMapper;
import com.jumbo.trus.service.exceptions.AuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class StepService {

    @Autowired
    private StepUpdateRepository stepUpdateRepository;

    @Autowired
    private StepUpdateMapper stepUpdateMapper;

    public List<StepUpdateDTO> getAllStepUpdates(StepFilter stepFilter){
        StepSpecification stepSpecification = new StepSpecification(stepFilter);
        return new ArrayList<>(stepUpdateRepository.findAll(stepSpecification, PageRequest.of(0, stepFilter.getLimit())).stream().map(stepUpdateMapper::toDTO).toList());
    }

    public StepDTO addStepUpdate(StepDTO stepDTO) {
        Long userId = getCurrentUser().getId();
        StepUpdateEntity stepUpdateEntity = stepUpdateRepository.findByUserId(userId).orElse(new StepUpdateEntity());
        stepUpdateEntity.setUpdateTime(new Date());
        stepUpdateEntity.setUserId(userId);
        stepUpdateEntity.setStepNumber(stepDTO.getStepNumber());
        StepUpdateEntity savedEntity = stepUpdateRepository.save(stepUpdateEntity);
        System.out.println(stepUpdateMapper.toDTO(savedEntity));
        return stepDTO;
        //return stepUpdateMapper.toDTO(savedEntity);
    }

    private UserEntity getCurrentUser() {
        try {
            return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }
}
