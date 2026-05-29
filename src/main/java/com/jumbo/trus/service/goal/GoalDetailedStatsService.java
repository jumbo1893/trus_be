package com.jumbo.trus.service.goal;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.goal.projection.IGoalAttendanceDetail;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GoalDetailedStatsService {

    private final GoalRepository goalRepository;

    public GoalDetailedResponse getAllDetailed(StatisticsFilter filter) {
        List<IGoalAttendanceDetail> rows = goalRepository.findGoalAttendanceDetails(
                filter.getAppTeam().getId(),
                normalizeSeasonId(filter.getSeasonId()),
                filter.getPlayerId(),
                filter.getMatchId(),
                normalizeFilter(filter.getStringFilter())
        );

        if (Boolean.TRUE.equals(filter.getMatchStatsOrPlayerStats())) {
            return buildMatchStats(rows);
        }

        return buildPlayerStats(rows);
    }

    private GoalDetailedResponse buildPlayerStats(List<IGoalAttendanceDetail> rows) {
        GoalDetailedResponse response = createBaseGoalResponse(rows);
        Map<Long, GoalDetailedDTO> playerStats = new HashMap<>();

        for (IGoalAttendanceDetail row : rows) {
            addToTotals(response, row);

            GoalDetailedDTO dto = playerStats.computeIfAbsent(row.getPlayerId(), playerId -> {
                GoalDetailedDTO newDto = new GoalDetailedDTO();
                newDto.setId(row.getPlayerId());
                newDto.setPlayer(toPlayerDTO(row));
                return newDto;
            });

            dto.addGoals(nullToZero(row.getGoalNumber()));
            dto.addAssists(nullToZero(row.getAssistNumber()));
        }

        List<GoalDetailedDTO> goalList = new ArrayList<>(playerStats.values());
        goalList.sort(goalPlayerComparator());
        response.setGoalList(goalList);
        return response;
    }

    private GoalDetailedResponse buildMatchStats(List<IGoalAttendanceDetail> rows) {
        GoalDetailedResponse response = createBaseGoalResponse(rows);
        Map<Long, GoalDetailedDTO> matchStats = new HashMap<>();

        for (IGoalAttendanceDetail row : rows) {
            addToTotals(response, row);

            GoalDetailedDTO dto = matchStats.computeIfAbsent(row.getMatchId(), matchId -> {
                GoalDetailedDTO newDto = new GoalDetailedDTO();
                newDto.setId(row.getMatchId());
                newDto.setMatch(toMatchDTO(row));
                return newDto;
            });

            dto.addGoals(nullToZero(row.getGoalNumber()));
            dto.addAssists(nullToZero(row.getAssistNumber()));
        }

        List<GoalDetailedDTO> goalList = new ArrayList<>(matchStats.values());
        goalList.sort(goalMatchComparator());
        response.setGoalList(goalList);
        return response;
    }

    private GoalDetailedResponse createBaseGoalResponse(List<IGoalAttendanceDetail> rows) {
        GoalDetailedResponse response = new GoalDetailedResponse();

        response.setPlayersCount(
                (int) rows.stream()
                        .map(IGoalAttendanceDetail::getPlayerId)
                        .distinct()
                        .count()
        );

        response.setMatchesCount(
                (int) rows.stream()
                        .map(IGoalAttendanceDetail::getMatchId)
                        .distinct()
                        .count()
        );

        response.setGoalList(new ArrayList<>());
        return response;
    }

    private void addToTotals(GoalDetailedResponse response, IGoalAttendanceDetail row) {
        response.addGoals(nullToZero(row.getGoalNumber()));
        response.addAssists(nullToZero(row.getAssistNumber()));
    }

    private PlayerDTO toPlayerDTO(IGoalAttendanceDetail row) {
        PlayerDTO player = new PlayerDTO();
        player.setId(row.getPlayerId());
        player.setName(row.getPlayerName());
        player.setBirthday(row.getPlayerBirthday());
        player.setFan(Boolean.TRUE.equals(row.getFan()));
        player.setActive(row.getActive() == null || Boolean.TRUE.equals(row.getActive()));
        return player;
    }

    private MatchDTO toMatchDTO(IGoalAttendanceDetail row) {
        MatchDTO match = new MatchDTO();
        match.setId(row.getMatchId());
        match.setName(row.getMatchName());
        match.setDate(row.getMatchDate());
        match.setSeasonId(row.getSeasonId());
        match.setHome(Boolean.TRUE.equals(row.getHome()));
        match.setHomeGoalNumber(nullToZero(row.getHomeGoalNumber()));
        match.setAwayGoalNumber(nullToZero(row.getAwayGoalNumber()));
        match.setPlayerIdList(Collections.emptyList());
        return match;
    }

    private Comparator<GoalDetailedDTO> goalPlayerComparator() {
        return Comparator.comparingInt((GoalDetailedDTO dto) -> dto.getGoalNumber() + dto.getAssistNumber())
                .reversed()
                .thenComparing(GoalDetailedDTO::getGoalNumber, Comparator.reverseOrder())
                .thenComparing(dto -> dto.getPlayer().getName());
    }

    private Comparator<GoalDetailedDTO> goalMatchComparator() {
        return Comparator.comparing(
                (GoalDetailedDTO dto) -> dto.getMatch().getDate(),
                Comparator.reverseOrder()
        );
    }

    private Long normalizeSeasonId(Long seasonId) {
        return seasonId == null ? Config.ALL_SEASON_ID : seasonId;
    }

    private String normalizeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        return filter.trim().toLowerCase(Locale.ROOT);
    }

    private int nullToZero(Integer number) {
        return number == null ? 0 : number;
    }
}
