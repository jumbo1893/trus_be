package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.specification.MatchSpecification;
import com.jumbo.trus.service.order.OrderMatchByDate;
import com.jumbo.trus.service.order.OrderPlayerByName;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Service
@RequiredArgsConstructor
public class MatchQueryService {

    public List<String> getStatisticsOpponents(Long appTeamId) {
        return matchRepository.findStatisticsOpponents(appTeamId);
    }

    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final PlayerMapper playerMapper;

    public List<MatchDTO> getAll(MatchFilter matchFilter) {
        MatchSpecification specification = new MatchSpecification(matchFilter);

        return matchRepository.findAll(
                        specification,
                        PageRequest.of(0, matchFilter.getLimit())
                ).stream()
                .map(matchMapper::toDTO)
                .sorted(new OrderMatchByDate())
                .toList();
    }

    public List<MatchEntity> getAllEntitiesBySeasonId(AppTeamEntity appTeam, Long seasonId) {
        if (seasonId == null || Objects.equals(seasonId, ALL_SEASON_ID)) {
            return matchRepository.getMatchesOrderByDateDesc(
                    appTeam.getId(),
                    PageRequest.of(0, 1000)
            );
        }

        return matchRepository.findAllBySeasonId(seasonId, appTeam.getId());
    }

    public List<MatchDTO> getMatchesByDate(int limit, boolean desc, long appTeamId) {
        List<MatchEntity> matches = desc
                ? matchRepository.getMatchesOrderByDateDesc(appTeamId, PageRequest.of(0, limit))
                : matchRepository.getMatchesOrderByDateAsc(appTeamId, PageRequest.of(0, limit));

        return matches.stream()
                .map(matchMapper::toDTO)
                .toList();
    }

    public List<PlayerDTO> getPlayerListByMatchId(Long matchId) {
        return getMatchEntity(matchId).getPlayerList().stream()
                .map(playerMapper::toDTO)
                .sorted(new OrderPlayerByName())
                .toList();
    }

    public List<PlayerDTO> getPlayerListByFilteredByFansByMatchId(Long matchId, boolean fan) {
        return getMatchEntity(matchId).getPlayerList().stream()
                .filter(player -> player.isFan() == fan)
                .map(playerMapper::toDTO)
                .sorted(new OrderPlayerByName())
                .toList();
    }

    public MatchDTO getMatch(long matchId) {
        return matchMapper.toDTO(getMatchEntity(matchId));
    }

    public MatchDTO getMatch(long matchId, long appTeamId) {
        return matchMapper.toDTO(getMatchEntity(matchId, appTeamId));
    }

    public MatchEntity getMatchEntity(long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Zápas s id " + matchId + " nebyl nalezen"
                ));
    }

    public MatchEntity getMatchEntity(long matchId, long appTeamId) {
        return matchRepository.findByIdAndAppTeamId(matchId, appTeamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Zápas s id " + matchId + " nebyl nalezen v týmu " + appTeamId
                ));
    }

    public MatchDTO getFirstMatchWherePlayerAttends(PlayerDTO player) {
        return matchRepository.findFirstMatchWherePlayerAttends(
                        player.getId(),
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .map(matchMapper::toDTO)
                .orElse(null);
    }

    public MatchDTO getLatestMatchBySeasonId(long seasonId, long appTeamId) {
        Pageable firstResult = PageRequest.of(0, 1);
        List<MatchEntity> matches = seasonId == ALL_SEASON_ID
                ? matchRepository.findLastByAppTeamId(appTeamId, firstResult)
                : matchRepository.findLastBySeasonId(seasonId, appTeamId, firstResult);

        return matches.stream()
                .findFirst()
                .map(matchMapper::toDTO)
                .orElse(null);
    }

    public List<Long> convertMatchesToIds(List<MatchDTO> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        return matches.stream()
                .map(MatchDTO::getId)
                .toList();
    }

    public MatchDTO findMatchByFootballMatchId(long footballMatchId, long appTeamId) {
        return matchRepository.findAllByFootballMatchId(footballMatchId, appTeamId)
                .map(matchMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Zápas pro footballMatchId " + footballMatchId + " nebyl nalezen"
                ));
    }

    public MatchDTO findMatchByFootballMatchIdOrNull(long footballMatchId, long appTeamId) {
        return matchRepository.findAllByFootballMatchId(footballMatchId, appTeamId)
                .map(matchMapper::toDTO)
                .orElse(null);
    }

    public MatchEntity findMatchByAroundTime(
            AppTeamEntity appTeam,
            Date startTime,
            Date endTime
    ) {
        Date from = Date.from(startTime.toInstant().minus(1, ChronoUnit.HOURS));
        Date to = Date.from(endTime.toInstant().plus(2, ChronoUnit.HOURS));

        return matchRepository.findMatchByTimeBetween(appTeam, from, to)
                .orElse(null);
    }
}
