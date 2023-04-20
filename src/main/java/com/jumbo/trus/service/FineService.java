package com.jumbo.trus.service;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.mapper.FineMapper;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.repository.FineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
public class FineService {

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private FineMapper fineMapper;

    public FineDTO addFine(FineDTO fineDTO) {
        FineEntity entity = fineMapper.toEntity(fineDTO);
        FineEntity savedEntity = fineRepository.save(entity);
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
        FineEntity entity = fineMapper.toEntity(fineDTO);
        entity.setId(fineId);
        FineEntity savedEntity = fineRepository.save(entity);
        return fineMapper.toDTO(savedEntity);
    }

    public void deleteFine(Long fineId) {
        fineRepository.deleteById(fineId);
    }
}
