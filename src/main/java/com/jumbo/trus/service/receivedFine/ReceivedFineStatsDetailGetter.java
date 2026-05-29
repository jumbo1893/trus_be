package com.jumbo.trus.service.receivedFine;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.receivedfine.response.stats.FineCountDTO;
import com.jumbo.trus.dto.receivedfine.response.stats.FineStatsDTO;
import com.jumbo.trus.dto.receivedfine.response.stats.match.*;
import com.jumbo.trus.dto.receivedfine.response.stats.player.*;
import com.jumbo.trus.dto.receivedfine.response.stats.projection.IMatchReceivedFineDetail;
import com.jumbo.trus.dto.receivedfine.response.stats.projection.IPlayerReceivedFineDetail;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.ReceivedFineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ReceivedFineStatsDetailGetter {

    private final ReceivedFineRepository receivedFineRepository;

    public ReceivedFineMatchDetailResponse getMatchDetail(Long matchId, AppTeamEntity appTeam) {
        List<IMatchReceivedFineDetail> rows =
                receivedFineRepository.findMatchFineDetail(matchId, appTeam.getId());

        Map<Long, MutablePlayerGroup> players = new LinkedHashMap<>();
        Map<Long, MutableFinePlayersGroup> fines = new LinkedHashMap<>();

        for (IMatchReceivedFineDetail row : rows) {
            StatsPlayerDTO player = new StatsPlayerDTO(row.getPlayerId(), row.getPlayerName());
            FineStatsDTO fine = new FineStatsDTO(
                    row.getFineId(),
                    row.getFineName(),
                    row.getFineAmount()
            );

            FineCountDTO fineCount = new FineCountDTO(
                    fine,
                    row.getFineCount(),
                    row.getTotalAmount()
            );

            players.computeIfAbsent(
                    row.getPlayerId(),
                    id -> new MutablePlayerGroup(player)
            ).add(fineCount);

            PlayerFineCountDTO playerFineCount = new PlayerFineCountDTO(
                    player,
                    row.getFineCount(),
                    row.getTotalAmount()
            );

            fines.computeIfAbsent(
                    row.getFineId(),
                    id -> new MutableFinePlayersGroup(fine)
            ).add(playerFineCount);
        }

        List<PlayerWithFinesDTO> playerList = players.values()
                .stream()
                .map(MutablePlayerGroup::toDTO)
                .sorted(Comparator.comparingLong(PlayerWithFinesDTO::getTotalAmount).reversed())
                .toList();

        List<FineWithPlayersDTO> fineList = fines.values()
                .stream()
                .map(MutableFinePlayersGroup::toDTO)
                .sorted(Comparator.comparingLong(FineWithPlayersDTO::getTotalAmount).reversed())
                .toList();

        return new ReceivedFineMatchDetailResponse(playerList, fineList);
    }

    public ReceivedFinePlayerDetailResponse getPlayerDetail(
            Long playerId,
            Long seasonId,
            AppTeamEntity appTeam
    ) {
        List<IPlayerReceivedFineDetail> rows =
                receivedFineRepository.findPlayerFineDetail(
                        playerId,
                        seasonId,
                        Config.ALL_SEASON_ID,
                        appTeam.getId()
                );

        Map<Long, MutableMatchGroup> matches = new LinkedHashMap<>();
        Map<Long, MutableFineMatchesGroup> fines = new LinkedHashMap<>();

        for (IPlayerReceivedFineDetail row : rows) {
            StatsMatchDTO match = new StatsMatchDTO(
                    row.getMatchId(),
                    row.getMatchName(),
                    row.getMatchDate(),
                    row.getSeasonId()
            );

            FineStatsDTO fine = new FineStatsDTO(
                    row.getFineId(),
                    row.getFineName(),
                    row.getFineAmount()
            );

            FineCountDTO fineCount = new FineCountDTO(
                    fine,
                    row.getFineCount(),
                    row.getTotalAmount()
            );

            matches.computeIfAbsent(
                    row.getMatchId(),
                    id -> new MutableMatchGroup(match)
            ).add(fineCount);

            MatchFineCountDTO matchFineCount = new MatchFineCountDTO(
                    match,
                    row.getFineCount(),
                    row.getTotalAmount()
            );

            fines.computeIfAbsent(
                    row.getFineId(),
                    id -> new MutableFineMatchesGroup(fine)
            ).add(matchFineCount);
        }

        List<MatchWithFinesDTO> matchList = matches.values()
                .stream()
                .map(MutableMatchGroup::toDTO)
                .sorted(Comparator.comparing(
                        dto -> dto.getMatch().getDate(),
                        Comparator.reverseOrder()
                ))
                .toList();

        List<FineWithMatchesDTO> fineList = fines.values()
                .stream()
                .map(MutableFineMatchesGroup::toDTO)
                .sorted(Comparator.comparingLong(FineWithMatchesDTO::getTotalAmount).reversed())
                .toList();

        return new ReceivedFinePlayerDetailResponse(matchList, fineList);
    }

    private static class MutablePlayerGroup {

        private final StatsPlayerDTO player;
        private final List<FineCountDTO> fines = new ArrayList<>();
        private long totalAmount;
        private long totalCount;

        private MutablePlayerGroup(StatsPlayerDTO player) {
            this.player = player;
        }

        private void add(FineCountDTO fine) {
            fines.add(fine);
            totalAmount += fine.getTotalAmount();
            totalCount += fine.getCount();
        }

        private PlayerWithFinesDTO toDTO() {
            return new PlayerWithFinesDTO(player, totalAmount, totalCount, fines);
        }
    }

    private static class MutableFinePlayersGroup {

        private final FineStatsDTO fine;
        private final List<PlayerFineCountDTO> players = new ArrayList<>();
        private long totalAmount;
        private long totalCount;

        private MutableFinePlayersGroup(FineStatsDTO fine) {
            this.fine = fine;
        }

        private void add(PlayerFineCountDTO player) {
            players.add(player);
            totalAmount += player.getTotalAmount();
            totalCount += player.getCount();
        }

        private FineWithPlayersDTO toDTO() {
            return new FineWithPlayersDTO(fine, totalAmount, totalCount, players);
        }
    }

    private static class MutableMatchGroup {

        private final StatsMatchDTO match;
        private final List<FineCountDTO> fines = new ArrayList<>();
        private long totalAmount;
        private long totalCount;

        private MutableMatchGroup(StatsMatchDTO match) {
            this.match = match;
        }

        private void add(FineCountDTO fine) {
            fines.add(fine);
            totalAmount += fine.getTotalAmount();
            totalCount += fine.getCount();
        }

        private MatchWithFinesDTO toDTO() {
            return new MatchWithFinesDTO(match, totalAmount, totalCount, fines);
        }
    }

    private static class MutableFineMatchesGroup {

        private final FineStatsDTO fine;
        private final List<MatchFineCountDTO> matches = new ArrayList<>();
        private long totalAmount;
        private long totalCount;

        private MutableFineMatchesGroup(FineStatsDTO fine) {
            this.fine = fine;
        }

        private void add(MatchFineCountDTO match) {
            matches.add(match);
            totalAmount += match.getTotalAmount();
            totalCount += match.getCount();
        }

        private FineWithMatchesDTO toDTO() {
            return new FineWithMatchesDTO(fine, totalAmount, totalCount, matches);
        }
    }
}