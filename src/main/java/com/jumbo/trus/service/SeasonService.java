package com.jumbo.trus.service;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.mapper.SeasonMapper;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.repository.SeasonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
public class SeasonService {

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private SeasonMapper seasonMapper;

    public SeasonDTO addSeason(SeasonDTO seasonDTO) {
        SeasonEntity entity = seasonMapper.toEntity(seasonDTO);
        SeasonEntity savedEntity = seasonRepository.save(entity);
        return seasonMapper.toDTO(savedEntity);
    }

    public List<SeasonDTO> getAll(int limit){
        List<SeasonEntity> seasonEntities = seasonRepository.getAll(limit);
        List<SeasonDTO> result = new ArrayList<>();
        for(SeasonEntity e : seasonEntities){
            result.add(seasonMapper.toDTO(e));
        }
        return result;
    }

    public SeasonDTO editSeason(Long seasonId, SeasonDTO seasonDTO) throws NotFoundException {
        if (!seasonRepository.existsById(seasonId)) {
            throw new NotFoundException("Sezona s id " + seasonId + " nenalezena v db");
        }
        SeasonEntity entity = seasonMapper.toEntity(seasonDTO);
        entity.setId(seasonId);
        SeasonEntity savedEntity = seasonRepository.save(entity);
        return seasonMapper.toDTO(savedEntity);
    }

    public void deleteSeason(Long seasonId) {
        seasonRepository.deleteById(seasonId);
    }
}
