package com.jumbo.trus.service.receivedFine;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.IPlayerFineStats;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.dto.receivedfine.response.get.setup.ReceivedFineSetupResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.mapper.ReceivedFineDetailedMapper;
import com.jumbo.trus.mapper.ReceivedFineMapper;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.repository.specification.ReceivedFineSpecification;
import com.jumbo.trus.repository.specification.ReceivedFineStatsSpecification;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.order.OrderReceivedFineDetailedDTOByFineAmount;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceivedFineGetter {

    private final ReceivedFineRepository receivedFineRepository;
    private final ReceivedFineDetailedMapper receivedFineDetailedMapper;
    private final ReceivedFineMapper receivedFineMapper;
    private final MatchService matchService;
    private final PlayerService playerService;
    private final FineService fineService;


    private record PlayerFineKey(Long playerId, Long fineId) {
    }

    public List<ReceivedFineDTO> getAll(ReceivedFineFilter receivedFineFilter) {
        ReceivedFineSpecification receivedFineSpecification = new ReceivedFineSpecification(receivedFineFilter);

        return receivedFineRepository
                .findAll(receivedFineSpecification, PageRequest.of(0, receivedFineFilter.getLimit()))
                .stream()
                .map(receivedFineMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ReceivedFineDetailedResponse getAllDetailed(StatisticsFilter filter) {
        ReceivedFineDetailedResponse response = new ReceivedFineDetailedResponse();

        List<ReceivedFineDetailedDTO> fineList = fetchFines(filter);

        Set<Long> matchSet = new HashSet<>();
        Set<Long> playerSet = new HashSet<>();

        Map<Long, ReceivedFineDetailedDTO> matchMap = new HashMap<>();
        Map<Long, ReceivedFineDetailedDTO> playerMap = new HashMap<>();
        Map<Long, ReceivedFineDetailedDTO> fineMap = new HashMap<>();

        Map<PlayerFineKey, ReceivedFineDetailedDTO> playerFineMap = new HashMap<>();

        for (ReceivedFineDetailedDTO fine : fineList) {
            updateResponseStats(response, fine);

            if (fine.getMatch() != null) {
                matchSet.add(fine.getMatch().getId());
            }

            if (fine.getPlayer() != null) {
                playerSet.add(fine.getPlayer().getId());
            }

            if (Boolean.TRUE.equals(filter.getDetailed())) {
                aggregateFineStats(fineMap, fine);
            } else if (Boolean.FALSE.equals(filter.getMatchStatsOrPlayerStats())) {
                if (Boolean.TRUE.equals(filter.getSplitPlayerFinesByFine())) {
                    aggregatePlayerFineStats(playerFineMap, fine);
                } else {
                    aggregatePlayerStats(playerMap, fine);
                }
            } else {
                aggregateMatchStats(matchMap, fine);
            }
        }

        List<ReceivedFineDetailedDTO> returnFineList = determineReturnList(
                filter,
                matchMap,
                playerMap,
                fineMap,
                playerFineMap,
                fineList
        );

        returnFineList.sort(new OrderReceivedFineDetailedDTOByFineAmount());

        response.setFineList(returnFineList);
        response.setMatchesCount(matchSet.size());
        response.setPlayersCount(playerSet.size());
        return response;
    }

    public ReceivedFineSetupResponse setupPlayers(ReceivedFineFilter receivedFineFilter) {
        PairSeasonMatch pairSeasonMatch = matchService.returnSeasonAndMatchByFilter(receivedFineFilter);
        SeasonDTO seasonDTO = pairSeasonMatch.getSeasonDTO();
        MatchDTO matchDTO = pairSeasonMatch.getMatchDTO();

        MatchFilter matchFilter = new MatchFilter();
        matchFilter.setSeasonId(seasonDTO.getId());
        matchFilter.setAppTeam(receivedFineFilter.getAppTeam());

        List<MatchDTO> matchList = matchService.getAll(matchFilter);

        List<PlayerDTO> playersInMatch = new ArrayList<>();
        List<PlayerDTO> otherPlayers = new ArrayList<>();

        if (matchDTO != null) {
            playersInMatch = matchService.getPlayerListByFilteredByFansByMatchId(matchDTO.getId(), false);
            otherPlayers = new ArrayList<>(playerService.getAllActive(true, receivedFineFilter.getAppTeam().getId()));
            otherPlayers.removeAll(playersInMatch);
        }

        return new ReceivedFineSetupResponse(matchDTO, seasonDTO, playersInMatch, otherPlayers, matchList);
    }

    public List<ReceivedFineDTO> getAllForSetup(Long playerId, Long matchId, AppTeamEntity appTeam) {
        if (playerId == null || matchId == null) {
            throw new EntityNotFoundException();
        }

        ReceivedFineFilter filter = new ReceivedFineFilter(matchId, playerId);
        filter.setAppTeam(appTeam);
        List<ReceivedFineDTO> receivedFines = getAll(new ReceivedFineFilter(matchId, playerId));
        List<Long> idList = getListOfFineIdsFromReceivedFines(receivedFines);
        List<FineDTO> fineDTOS;
        if (idList.isEmpty()) {
            fineDTOS = fineService.getAll(10000000, appTeam.getId());
        } else {
            fineDTOS = fineService.getFinesExcluding(idList, appTeam.getId());
        }

        receivedFines.addAll(makeReceivedFineSetupListFromFineList(fineDTOS, playerId, matchId));

        return receivedFines;
    }

    public List<ReceivedFineDTO> getReceivedFinesInMatchesByFineNameAndPlayer(
            Long playerId,
            List<Long> matchIds,
            String fineName,
            long appTeamId
    ) {
        Long fineId = fineService.getFineByName(fineName, appTeamId).getId();

        return receivedFineRepository
                .findAllByPlayerIdFineIdAndMatchesId(playerId, fineId, matchIds)
                .stream()
                .map(receivedFineMapper::toDTO)
                .toList();
    }

    public Integer getAtLeastNumberOfFineInHistory(Long playerId, String fineName, int fineNumber) {
        return receivedFineRepository.findAtLeastNumberOfFineInHistory(playerId, fineName, fineNumber);
    }

    public ReceivedFineDTO getFirstOccurrenceOfFine(Long playerId, String fineName) {
        return receivedFineRepository
                .findFirstOccurrenceOfFine(playerId, fineName)
                .map(receivedFineMapper::toDTO)
                .orElse(null);
    }

    private List<Long> getListOfFineIdsFromReceivedFines(List<ReceivedFineDTO> receivedFines) {
        return receivedFines
                .stream()
                .map(fine -> fine.getFine().getId())
                .collect(Collectors.toList());
    }

    private List<ReceivedFineDTO> makeReceivedFineSetupListFromFineList(
            List<FineDTO> fineDTOS,
            Long playerId,
            Long matchId
    ) {
        return fineDTOS
                .stream()
                .map(fine -> new ReceivedFineDTO(0, fine, playerId, matchId))
                .collect(Collectors.toList());
    }

    private void updateResponseStats(ReceivedFineDetailedResponse response, ReceivedFineDetailedDTO fine) {
        response.addFines(fine.getFineNumber());
        response.addFineAmount(fine.returnFineAmount());
    }

    private void aggregateFineStats(
            Map<Long, ReceivedFineDetailedDTO> fineMap,
            ReceivedFineDetailedDTO fine
    ) {
        if (fine.getFine() == null) {
            return;
        }
        fine.setMatch(null);
        fine.setPlayer(null);
        fineMap.merge(fine.getFine().getId(), fine, this::mergeFines);
    }

    private void aggregateMatchStats(
            Map<Long, ReceivedFineDetailedDTO> matchMap,
            ReceivedFineDetailedDTO fine
    ) {
        if (fine.getMatch() == null) {
            return;
        }
        fine.setPlayer(null);
        matchMap.merge(fine.getMatch().getId(), fine, this::mergeFines);
    }

    private void aggregatePlayerStats(
            Map<Long, ReceivedFineDetailedDTO> playerMap,
            ReceivedFineDetailedDTO fine
    ) {
        if (fine.getPlayer() == null) {
            return;
        }

        fine.setMatch(null);
        playerMap.merge(fine.getPlayer().getId(), fine, this::mergeFines);
    }

    private void aggregatePlayerFineStats(
            Map<PlayerFineKey, ReceivedFineDetailedDTO> playerFineMap,
            ReceivedFineDetailedDTO fine
    ) {
        if (fine.getPlayer() == null || fine.getFine() == null) {
            return;
        }
        fine.setMatch(null);
        PlayerFineKey key = new PlayerFineKey(
                fine.getPlayer().getId(),
                fine.getFine().getId()
        );
        playerFineMap.merge(key, fine, this::mergeFines);
    }

    private ReceivedFineDetailedDTO mergeFines(
            ReceivedFineDetailedDTO existing,
            ReceivedFineDetailedDTO newFine
    ) {
        existing.addFineNumber(newFine.getFineNumber());
        existing.addFineAmount(newFine.returnFineAmount());
        return existing;
    }

    private List<ReceivedFineDetailedDTO> determineReturnList(
            StatisticsFilter filter,
            Map<Long, ReceivedFineDetailedDTO> matchMap,
            Map<Long, ReceivedFineDetailedDTO> playerMap,
            Map<Long, ReceivedFineDetailedDTO> fineMap,
            Map<PlayerFineKey, ReceivedFineDetailedDTO> playerFineMap,
            List<ReceivedFineDetailedDTO> fineList
    ) {
        if (Boolean.TRUE.equals(filter.getDetailed())) {
            return new ArrayList<>(fineMap.values());
        }

        if (Boolean.TRUE.equals(filter.getMatchStatsOrPlayerStats())) {
            return new ArrayList<>(matchMap.values());
        }

        if (Boolean.FALSE.equals(filter.getMatchStatsOrPlayerStats())) {
            if (Boolean.TRUE.equals(filter.getSplitPlayerFinesByFine())) {
                return new ArrayList<>(playerFineMap.values());
            }

            return new ArrayList<>(playerMap.values());
        }

        return new ArrayList<>(fineList);
    }

    private List<ReceivedFineDetailedDTO> fetchFines(StatisticsFilter filter) {
        ReceivedFineStatsSpecification specification = new ReceivedFineStatsSpecification(filter);

        return receivedFineRepository
                .findAll(specification, PageRequest.of(0, filter.getLimit()))
                .stream()
                .map(receivedFineDetailedMapper::toDTO)
                .toList();
    }

    public List<IPlayerFineStats> getListOfPlayersOrderByReceivedFineAmount(
            long seasonId,
            long appTeamId,
            int count
    ) {
        if (seasonId == ALL_SEASON_ID) {
            return receivedFineRepository.findTopFineStatsByAppTeam(
                    appTeamId,
                    PageRequest.of(0, count)
            );
        }

        return receivedFineRepository.findTopFineStatsByAppTeamAndSeason(
                appTeamId,
                seasonId,
                PageRequest.of(0, count)
        );
    }
}