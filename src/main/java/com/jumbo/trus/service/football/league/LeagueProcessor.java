package com.jumbo.trus.service.football.league;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.entity.repository.football.LeagueRepository;
import com.jumbo.trus.mapper.football.LeagueMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueProcessor {

    private final LeagueRepository leagueRepository;
    private final LeagueMapper leagueMapper;

    public boolean isNewLeague(LeagueDTO league) {
        return !leagueRepository.existsByUri(league.getUri());
    }

    public void saveNewLeagueToRepository(LeagueDTO leagueFromWeb) {
        leagueRepository.save(leagueMapper.toEntity(leagueFromWeb));
    }

    @Transactional
    public int updateLeagueIfNeeded(LeagueDTO leagueFromWeb) {
        int updatedLeagues = 0;
        log.debug("liga {}", leagueFromWeb);
        LeagueDTO currentLeague = leagueMapper.toDTO(leagueRepository.findByUri(leagueFromWeb.getUri()));
        if (!currentLeague.equals(leagueFromWeb)) {
            updatedLeagues += leagueRepository.updateLeagueFields(
                    currentLeague.getId(),
                    leagueFromWeb.getName(),
                    leagueFromWeb.getRank(),
                    leagueFromWeb.getYear(),
                    leagueFromWeb.isCurrentLeague()
            );
        }
        return updatedLeagues;
    }
}
