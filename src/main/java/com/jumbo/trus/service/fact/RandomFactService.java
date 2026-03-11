package com.jumbo.trus.service.fact;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.DynamicTextEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.lock.LockManager;
import com.jumbo.trus.repository.DynamicTextRepository;
import com.jumbo.trus.service.SeasonService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class RandomFactService {

    private final SeasonService seasonService;
    private final BeerFactService beerFactService;
    private final FineFactService fineFactService;
    private final DynamicTextRepository dynamicTextRepository;
    private final LockManager lockManager;

    private static final String randomFactName = "RANDOM_FACT";


    public List<String> getRandomFacts(AppTeamEntity appTeam) {
        return dynamicTextRepository.findTextByNameAndAppTeam(randomFactName, appTeam);
    }

    private List<String> updateRandomFacts(AppTeamEntity appTeam) {
        SeasonDTO currentSeason = seasonService.getCurrentSeason(true, appTeam);
        StatisticsFilter allSeasonPlayerFilter = new StatisticsFilter(null, null, Config.ALL_SEASON_ID, false, appTeam);
        StatisticsFilter allSeasonMatchFilter = new StatisticsFilter(null, null, Config.ALL_SEASON_ID, true, appTeam);
        StatisticsFilter currentSeasonMatchFilter = new StatisticsFilter(null, null, currentSeason.getId(), true, appTeam);
        List<String> returnList = new ArrayList<>(beerFactService.returnBeerFacts(allSeasonPlayerFilter, allSeasonMatchFilter, currentSeasonMatchFilter));
        returnList.addAll(fineFactService.returnFineFacts(allSeasonPlayerFilter, allSeasonMatchFilter, currentSeasonMatchFilter));
        return returnList;
    }

    @Async
    @Transactional
    public void saveOrUpdateRandomFacts(AppTeamEntity appTeam) {
        ReentrantLock lock = lockManager.getLock(appTeam.getId());
        if (lock.tryLock()) { // Non-blocking pokus – pokud zamčené, počkáme (fair lock)
            try {
                calculateRandomFacts(appTeam);
            } finally {
                lock.unlock();
            }
        } else {
            // Pokud nelze získat lock ihned, zařaď do fronty nebo retry – ale pro jednoduchost: čekej blokujícím způsobem
            lock.lock(); // Blokující čekání
            try {
                calculateRandomFacts(appTeam);
            } finally {
                lock.unlock();
            }
        }
    }

    private void calculateRandomFacts(AppTeamEntity appTeam) {
        List<String> facts = updateRandomFacts(appTeam);
        // UPDATE nebo CREATE
        for (int i = 0; i < facts.size(); i++) {
            String text = facts.get(i);

            Optional<DynamicTextEntity> existing = dynamicTextRepository
                    .findByNameAndRankAndAppTeam(randomFactName, i, appTeam);
            DynamicTextEntity entity = existing.orElseGet(DynamicTextEntity::new);
            entity.setName(randomFactName);
            entity.setRank(i);
            entity.setAppTeam(appTeam);
            entity.setText(text);
            dynamicTextRepository.save(entity);
        }
        dynamicTextRepository.deleteByNameAndAppTeamAndRankGreaterThanEqual(
                randomFactName, appTeam, facts.size());
    }

}
