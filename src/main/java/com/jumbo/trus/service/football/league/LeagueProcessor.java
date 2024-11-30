package com.jumbo.trus.service.football.league;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.entity.repository.football.LeagueRepository;
import com.jumbo.trus.mapper.football.LeagueMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LeagueProcessor {

    private final LeagueRepository leagueRepository;
    private final LeagueMapper leagueMapper;

    public boolean isNewLeague(LeagueDTO league) {
        return !leagueRepository.existsByUri(league.getUri());
    }

    public int saveNewLeaguesToRepository(List<LeagueDTO> leaguesFromWeb) {
        leaguesFromWeb.forEach(league -> leagueRepository.save(leagueMapper.toEntity(league)));
        return leaguesFromWeb.size();
    }

    @Transactional
    public int updateLeagueIfNeeded(List<LeagueDTO> leaguesFromWeb) {
        int updatedLeagues = 0;
        for (LeagueDTO newLeague : leaguesFromWeb) {
            LeagueDTO currentLeague = leagueMapper.toDTO(leagueRepository.findByUri(newLeague.getUri()));
            if (!currentLeague.equals(newLeague)) {
                updatedLeagues += leagueRepository.updateLeagueFields(
                    currentLeague.getId(),
                    newLeague.getName(),
                    newLeague.getRank(),
                    newLeague.getYear(),
                    newLeague.isCurrentLeague()
                );
            }
        }
        return updatedLeagues;
    }
}
