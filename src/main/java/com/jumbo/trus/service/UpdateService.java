package com.jumbo.trus.service;

import com.jumbo.trus.dto.UpdateDTO;
import com.jumbo.trus.entity.repository.UpdateRepository;
import com.jumbo.trus.mapper.UpdateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UpdateService {

    Logger logger = LoggerFactory.getLogger(UpdateService.class);

    @Autowired
    private UpdateRepository updateRepository;

    @Autowired
    UpdateMapper updateMapper;

    public UpdateDTO getUpdateByName(String name) {
        return updateMapper.toDTO(updateRepository.getUpdateByName(name));
    }

    public void saveNewUpdate(UpdateDTO updateDTO) {
        updateRepository.save(updateMapper.toEntity(updateDTO));
    }

    public void saveNewUpdate(String name) {
        UpdateDTO updateDTO = new UpdateDTO();
        updateDTO.setName(name);
        updateDTO.setDate(new Date());
        saveNewUpdate(updateDTO);
    }
}
