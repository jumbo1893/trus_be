package com.jumbo.trus.service;

import com.jumbo.trus.dto.UpdateDTO;
import com.jumbo.trus.mapper.UpdateMapper;
import com.jumbo.trus.repository.UpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UpdateService {

    private final UpdateRepository updateRepository;
    private final UpdateMapper updateMapper;

    public UpdateDTO getUpdateByNameAndAppTeamId(String name, Long appTeamId) {
        return updateMapper.toDTO(updateRepository.findByNameAndAppTeamId(name, appTeamId).orElse(null));
    }

    public UpdateDTO getUpdateByName(String name) {
        return updateMapper.toDTO(updateRepository.getUpdateByName(name));
    }

    public UpdateDTO saveNewUniqueUpdate(UpdateDTO updateDTO) {
        return updateMapper.toDTO(updateRepository.save(updateMapper.toEntity(updateDTO)));
    }

    public void saveNewUniqueUpdate(String name) {
        if (getUpdateByName(name) != null) {
            return;
        }
        UpdateDTO updateDTO = new UpdateDTO();
        updateDTO.setName(name);
        updateDTO.setDate(new Date());
        saveNewUniqueUpdate(updateDTO);
    }

    public UpdateDTO saveNewUniqueUpdate(String name, Long appTeamId) {
        UpdateDTO updateDTO = getUpdateByNameAndAppTeamId(name, appTeamId);
        if (updateDTO == null) {
            updateDTO = new UpdateDTO();
        }
        updateDTO.setName(name);
        updateDTO.setAppTeamId(appTeamId);
        updateDTO.setDate(new Date());
        return saveNewUniqueUpdate(updateDTO);
    }
}
