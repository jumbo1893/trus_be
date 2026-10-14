package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.football.detail.ForeignNameEstimate;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.repository.football.FootballMatchPlayerRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.football.FootballPlayerRepository;
import com.jumbo.trus.service.achievement.calendar.CzechCelebrationCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ForeignNameEstimateService {
    private final FootballPlayerRepository playerRepository;
    private final FootballMatchRepository matchRepository;
    private final FootballMatchPlayerRepository matchPlayerRepository;
    private final CzechCelebrationCalendar calendar;

    public ForeignNameEstimate forTeam(long teamId) {
        List<FootballPlayerEntity> roster = playerRepository.findAllByTeamId(teamId);
        var lastMatch = matchRepository.findLastPlayedMatch(teamId);
        List<FootballPlayerEntity> lastPlayers = lastMatch == null ? List.of()
                : matchPlayerRepository.findDistinctPlayersByTeamAndMatch(teamId, lastMatch.getId());
        return new ForeignNameEstimate(percent(roster), percent(lastPlayers));
    }

    private Integer percent(List<FootballPlayerEntity> players) {
        if (players.isEmpty()) return null;
        long foreignNames = players.stream().map(FootballPlayerEntity::getName)
                .filter(name -> name != null && !name.isBlank() && !calendar.hasCzechFirstName(name))
                .count();
        return (int) Math.round(100.0 * foreignNames / players.size());
    }
}
