package com.jumbo.trus.service.football.pkfl;

import com.jumbo.trus.dto.pkfl.PkflSeasonDTO;
import com.jumbo.trus.entity.pkfl.PkflSeasonEntity;
import com.jumbo.trus.entity.repository.PkflSeasonRepository;
import com.jumbo.trus.mapper.pkfl.PkflSeasonMapper;
import com.jumbo.trus.service.football.pkfl.task.RetrieveSeasonUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PkflSeasonService {


    @Value("${pkfl.trus}")
    private String trusUrl;

    @Autowired
    private PkflSeasonRepository pkflSeasonRepository;

    @Autowired
    private PkflSeasonMapper pkflSeasonMapper;

    public List<PkflSeasonDTO> getAllSeasons() {
        List<PkflSeasonDTO> repositorySeasons = getCurrentSeasonsFromRepository();
        if (repositorySeasons.size() > 5) { //jinak to už bylo určitě aktualizovaný
            return repositorySeasons;
        }
        List<PkflSeasonDTO> webSeasons = getAllSeasonsFromWeb();
        saveSeasonsToRepository(webSeasons);
        return getAllSeasonsFromRepository();
    }

    private List<PkflSeasonDTO> getAllSeasonsFromWeb() {
        return new RetrieveSeasonUrl().getSeasonUrls(trusUrl);
    }

    public List<PkflSeasonDTO> getCurrentSeasons() {
        List<PkflSeasonDTO> repositorySeasons = getCurrentSeasonsFromRepository();
        if (repositorySeasons.size() > 1) {
            return repositorySeasons;
        }
        List<PkflSeasonDTO> webSeasons = getCurrentSeasonsFromWeb();
        saveSeasonsToRepository(webSeasons);
        return getCurrentSeasonsFromRepository();
    }

    private List<PkflSeasonDTO> getCurrentSeasonsFromRepository() {
        return pkflSeasonRepository.getSeasonByNameLike(getCurrentSeasonString()).stream().map(pkflSeasonMapper::toDTO).toList();
    }

    private List<PkflSeasonDTO> getAllSeasonsFromRepository() {
        return pkflSeasonRepository.findAll().stream().map(pkflSeasonMapper::toDTO).toList();
    }

    private List<PkflSeasonDTO> getCurrentSeasonsFromWeb() {

        return new RetrieveSeasonUrl().getCurrentSeasonUrls(trusUrl, getCurrentSeasonString());
    }

    private void saveSeasonsToRepository(List<PkflSeasonDTO> seasons) {
        for (PkflSeasonDTO season : seasons) {
            if (!pkflSeasonRepository.existsByName(season.getName())) {
                PkflSeasonEntity entity = pkflSeasonMapper.toEntity(season);
                pkflSeasonRepository.save(entity);
            }
        }
    }

    private String getCurrentSeasonString() {
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        if (currentDate.getMonthValue() < 7) {
            return (year-1) + "/" + year;
        }
        return year + "/" + (year+1);
    }

}
