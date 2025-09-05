package com.jumbo.trus.service;

import com.jumbo.trus.dto.UpdateDTO;
import com.jumbo.trus.mapper.UpdateMapper;
import com.jumbo.trus.repository.UpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UpdateService {

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
        if (getUpdateByName(name) != null) {
            return;
        }
        UpdateDTO updateDTO = new UpdateDTO();
        updateDTO.setName(name);
        updateDTO.setDate(new Date());
        saveNewUpdate(updateDTO);
    }
}
