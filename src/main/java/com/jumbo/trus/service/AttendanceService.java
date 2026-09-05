package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.attendance.AttendanceDetailedDTO;
import com.jumbo.trus.dto.attendance.AttendanceDetailedResponse;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.specification.StatisticsPredicates;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final PlayerMapper playerMapper;

    public AttendanceDetailedResponse getAllDetailed(StatisticsFilter filter) {
        List<MatchEntity> matches = getMatches(filter);

        if (filter.getPlayerId() != null) {
            return getMatchesForPlayer(filter.getPlayerId(), matches, filter);
        }

        if (filter.getMatchId() != null) {
            return getPlayersForMatch(filter.getMatchId(), matches, filter);
        }

        if (Boolean.TRUE.equals(filter.getMatchStatsOrPlayerStats())) {
            return getMatchAttendance(matches, filter);
        }

        return getPlayerAttendance(matches, filter);
    }

    private List<MatchEntity> getMatches(StatisticsFilter filter) {
        return matchRepository.findAll((root, query, cb) -> {
            root.fetch("playerList", JoinType.LEFT);
            query.distinct(true);
            query.orderBy(cb.desc(root.get("date")));
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("appTeam").get("id"), filter.getAppTeam().getId()));
            if (filter.getSeasonId() != null && filter.getSeasonId() != Config.ALL_SEASON_ID) {
                predicates.add(cb.equal(root.get("season").get("id"), filter.getSeasonId()));
            }
            if (filter.getMatchId() != null) {
                predicates.add(cb.equal(root.get("id"), filter.getMatchId()));
            }
            StatisticsPredicates.addMatch(predicates, root, cb, filter);
            if (Boolean.TRUE.equals(filter.getMatchStatsOrPlayerStats())
                    && filter.getStringFilter() != null && !filter.getStringFilter().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + filter.getStringFilter().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    private AttendanceDetailedResponse getPlayerAttendance(
            List<MatchEntity> matches,
            StatisticsFilter filter
    ) {
        Map<Long, AttendanceDetailedDTO> resultMap = new HashMap<>();
        Set<Long> matchIds = new HashSet<>();

        for (MatchEntity match : matches) {
            matchIds.add(match.getId());

            for (PlayerEntity player : match.getPlayerList()) {
                if (matchesPlayer(player, filter)) {
                    resultMap.compute(player.getId(), (id, oldValue) -> {
                        if (oldValue == null) {
                            AttendanceDetailedDTO dto = new AttendanceDetailedDTO();
                            dto.setId(player.getId());
                            dto.setPlayer(playerMapper.toDTO(player));
                            dto.setAttendanceCount(1);
                            dto.setTotalCount(1);
                            return dto;
                        }

                        oldValue.setAttendanceCount(oldValue.getAttendanceCount() + 1);
                        oldValue.setTotalCount(oldValue.getAttendanceCount());
                        return oldValue;
                    });
                }
            }
        }

        List<AttendanceDetailedDTO> list = new ArrayList<>(resultMap.values());
        list.sort(
                Comparator.comparingInt(AttendanceDetailedDTO::getAttendanceCount)
                        .reversed()
                        .thenComparing(dto -> dto.getPlayer().getName())
        );

        AttendanceDetailedResponse response = new AttendanceDetailedResponse();
        response.setAttendanceList(list);
        response.setPlayersCount(list.size());
        response.setMatchesCount(matchIds.size());
        return response;
    }

    private AttendanceDetailedResponse getMatchAttendance(List<MatchEntity> matches, StatisticsFilter filter) {
        List<AttendanceDetailedDTO> list = matches.stream()
                .map(match -> {
                    int playerCount = 0;
                    int fanCount = 0;

                    for (PlayerEntity player : match.getPlayerList()) {
                        if (!matchesPlayer(player, filter)) continue;
                        if (player.isFan()) {
                            fanCount++;
                        } else {
                            playerCount++;
                        }
                    }

                    AttendanceDetailedDTO dto = new AttendanceDetailedDTO();
                    dto.setId(match.getId());
                    dto.setMatch(matchMapper.toDTO(match));
                    dto.setPlayerCount(playerCount);
                    dto.setFanCount(fanCount);
                    dto.setTotalCount(playerCount + fanCount);
                    dto.setAttendanceCount(playerCount + fanCount);
                    return dto;
                })
                .filter(dto -> filter.getPlayerIds().isEmpty() || dto.getTotalCount() > 0)
                .sorted(
                        Comparator.comparing(
                                dto -> dto.getMatch().getDate(),
                                Comparator.reverseOrder()
                        )
                )
                .toList();

        Set<Long> uniquePlayers = matches.stream()
                .flatMap(match -> match.getPlayerList().stream())
                .filter(player -> matchesPlayer(player, filter))
                .map(PlayerEntity::getId)
                .collect(Collectors.toSet());

        AttendanceDetailedResponse response = new AttendanceDetailedResponse();
        response.setAttendanceList(list);
        response.setPlayersCount(uniquePlayers.size());
        response.setMatchesCount(list.size());
        return response;
    }

    private AttendanceDetailedResponse getMatchesForPlayer(Long playerId, List<MatchEntity> matches, StatisticsFilter filter) {
        List<AttendanceDetailedDTO> list = matches.stream()
                .filter(match -> match.getPlayerList()
                        .stream()
                        .anyMatch(player -> Objects.equals(player.getId(), playerId) && matchesPlayer(player, filter)))
                .map(match -> {
                    AttendanceDetailedDTO dto = new AttendanceDetailedDTO();
                    dto.setId(match.getId());
                    dto.setMatch(matchMapper.toDTO(match));
                    dto.setAttendanceCount(1);
                    dto.setTotalCount(1);
                    return dto;
                })
                .sorted(
                        Comparator.comparing(
                                dto -> dto.getMatch().getDate(),
                                Comparator.reverseOrder()
                        )
                )
                .toList();

        AttendanceDetailedResponse response = new AttendanceDetailedResponse();
        response.setAttendanceList(list);
        response.setPlayersCount(list.isEmpty() ? 0 : 1);
        response.setMatchesCount(list.size());
        return response;
    }

    private AttendanceDetailedResponse getPlayersForMatch(Long matchId, List<MatchEntity> matches, StatisticsFilter filter) {
        MatchEntity selectedMatch = matches.stream()
                .filter(match -> Objects.equals(match.getId(), matchId))
                .findFirst()
                .orElse(null);

        if (selectedMatch == null) {
            AttendanceDetailedResponse empty = new AttendanceDetailedResponse();
            empty.setAttendanceList(List.of());
            empty.setPlayersCount(0);
            empty.setMatchesCount(0);
            return empty;
        }

        List<AttendanceDetailedDTO> list = selectedMatch.getPlayerList()
                .stream()
                .filter(player -> matchesPlayer(player, filter))
                .sorted(
                        Comparator.comparing(PlayerEntity::isFan)
                                .thenComparing(PlayerEntity::getName)
                )
                .map(player -> {
                    AttendanceDetailedDTO dto = new AttendanceDetailedDTO();
                    dto.setId(player.getId());
                    dto.setPlayer(playerMapper.toDTO(player));
                    dto.setAttendanceCount(1);
                    dto.setTotalCount(1);
                    return dto;
                })
                .toList();

        AttendanceDetailedResponse response = new AttendanceDetailedResponse();
        response.setAttendanceList(list);
        response.setPlayersCount(list.size());
        response.setMatchesCount(1);
        return response;
    }

    private boolean matchesFilter(PlayerEntity player, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }

        return player.getName()
                .toLowerCase(Locale.ROOT)
                .contains(filter.toLowerCase(Locale.ROOT));
    }

    private boolean matchesPlayer(PlayerEntity player, StatisticsFilter filter) {
        return (filter.getPlayerIds().isEmpty() || filter.getPlayerIds().contains(player.getId()))
                && (!Boolean.FALSE.equals(filter.getMatchStatsOrPlayerStats())
                    || matchesFilter(player, filter.getStringFilter()));
    }
}
