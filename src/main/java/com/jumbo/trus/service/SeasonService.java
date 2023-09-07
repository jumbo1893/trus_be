package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.repository.MatchRepository;
import com.jumbo.trus.mapper.SeasonMapper;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.repository.SeasonRepository;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.helper.ValidationField;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jumbo.trus.config.Config.*;

@Service
public class SeasonService {

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SeasonMapper seasonMapper;

    public SeasonDTO addSeason(SeasonDTO seasonDTO) {
        validateSeason(seasonDTO.getFromDate(), seasonDTO.getFromDate(), null);
        SeasonEntity entity = seasonMapper.toEntity(seasonDTO);
        SeasonEntity savedEntity = seasonRepository.save(entity);
        return seasonMapper.toDTO(savedEntity);
    }

    public List<SeasonDTO> getAll(SeasonFilter seasonFilter){
        List<SeasonEntity> seasonEntities = seasonRepository.getAllWithoutNonEditable(seasonFilter.getLimit());
        List<SeasonDTO> result = new ArrayList<>();
        for(SeasonEntity e : seasonEntities){
            result.add(seasonMapper.toDTO(e));
        }
        if (seasonFilter.isAllSeason()) {
            result.add(0, getAllSeason());
        }
        if (seasonFilter.isOtherSeason()) {
            result.add(getOtherSeason());
        }
        if (seasonFilter.isAutomaticSeason()) {
            result.add(0, getAutomaticSeason());
        }
        return result;
    }

    public SeasonDTO getSeason(Long seasonId) {
        if (seasonId == AUTOMATIC_SEASON_ID) {
            return getAutomaticSeason();
        } else if (seasonId == OTHER_SEASON_ID) {
            return getOtherSeason();
        } else if (seasonId == ALL_SEASON_ID) {
            return getAllSeason();
        }
        SeasonEntity seasonEntity = seasonRepository.findById(seasonId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(seasonId)));
        return seasonMapper.toDTO(seasonEntity);
    }

    public SeasonDTO getCurrentSeason() {
        List<SeasonDTO> seasonList = getAll(new SeasonFilter());
        for (SeasonDTO season : seasonList) {
            if (!season.getFromDate().after(new Date()) && !season.getToDate().before(new Date())) {
                return season;
            }
        }
        return getOtherSeason();
    }

    public SeasonDTO editSeason(Long seasonId, SeasonDTO seasonDTO) throws NotFoundException {
        if (!seasonRepository.existsById(seasonId)) {
            throw new NotFoundException("Sezona s id " + seasonId + " nenalezena v db");
        }
        validateSeason(seasonDTO.getFromDate(), seasonDTO.getFromDate(), seasonDTO);
        SeasonEntity entity = seasonMapper.toEntity(seasonDTO);
        entity.setId(seasonId);
        SeasonEntity savedEntity = seasonRepository.save(entity);
        return seasonMapper.toDTO(savedEntity);
    }

    @Transactional
    public void deleteSeason(Long seasonId) {
        seasonRepository.updateSeasonId(seasonId);
        matchRepository.updateSeasonId(seasonId);
        seasonRepository.deleteById(seasonId);
    }

    private void validateSeason(Date fromDate, Date toDate, SeasonDTO currentSeason) {
        List<SeasonDTO> seasons = getAll(new SeasonFilter(false, false, false));
        for (SeasonDTO season : seasons) {
            if (currentSeason != null && currentSeason.getId() != season.getId()) {
                if (isSeasonCollision(season, fromDate, toDate)) {
                    makeValidationException(season);
                }
            } else if (currentSeason == null) {
                if (isSeasonCollision(season, fromDate, toDate)) {
                    makeValidationException(season);
                }
            }

        }
    }

    private boolean isSeasonCollision(SeasonDTO season, Date fromDate, Date toDate) {
        return ((!fromDate.before(season.getFromDate()) && !fromDate.after(season.getToDate())) ||
                (!toDate.before(season.getFromDate()) && !toDate.after(season.getToDate())) ||
                (!fromDate.after(season.getFromDate()) && !toDate.before(season.getToDate())));
    }

    private void makeValidationException(SeasonDTO season) {
        List<ValidationField> fields = new ArrayList<>();
        fields.add(new ValidationField("fromDate", "Datum se kryje se sezonou " + season.getName()));
        fields.add(new ValidationField("toDate", "Datum se kryje se sezonou " + season.getName()));
        throw new FieldValidationException("Chyba při validaci sezon", fields);
    }

    public SeasonDTO getAutomaticSeason() {
        return new SeasonDTO(AUTOMATIC_SEASON_ID, Config.AUTOMATIC_SEASON_NAME, Config.AUTOMATIC_SEASON_DATE, Config.AUTOMATIC_SEASON_DATE );
    }

    public SeasonDTO getOtherSeason() {
        return new SeasonDTO(OTHER_SEASON_ID, Config.OTHER_SEASON_NAME, Config.OTHER_SEASON_DATE, Config.OTHER_SEASON_DATE );
    }

    public SeasonDTO getAllSeason() {
        return new SeasonDTO(ALL_SEASON_ID, Config.ALL_SEASON_NAME, Config.ALL_SEASON_DATE, Config.ALL_SEASON_DATE );
    }
}
