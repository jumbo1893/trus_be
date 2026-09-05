package com.jumbo.trus.service.receivedFine;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.receivedfine.response.stats.projection.IMatchReceivedFineDetail;
import com.jumbo.trus.dto.receivedfine.response.stats.projection.IPlayerReceivedFineDetail;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.repository.ReceivedFineRepository;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReceivedFineStatsDetailGetterTest {
    private final ReceivedFineRepository repository = mock(ReceivedFineRepository.class);
    private final ReceivedFineStatsDetailGetter getter = new ReceivedFineStatsDetailGetter(repository);
    private final AppTeamEntity team = new AppTeamEntity();

    @Test
    void matchDetailCombinesPlayerAndFineSelectionsBeforeTotals() {
        team.setId(1L);
        doReturn(List.of(
            matchRow(1L, 5L, 20L), matchRow(2L, 6L, 30L),
            matchRow(3L, 5L, 40L), matchRow(1L, 7L, 50L))).when(repository).findMatchFineDetail(10L, 1L);
        StatisticsFilter filter = new StatisticsFilter();
        filter.setPlayerIds(List.of(1L, 2L));
        filter.setFineIds(List.of(5L, 6L));
        var response = getter.getMatchDetail(10L, team, filter);
        assertThat(response.getPlayers()).hasSize(2);
        assertThat(response.getFines()).hasSize(2);
        assertThat(response.getPlayers().stream().mapToLong(p -> p.getTotalAmount()).sum()).isEqualTo(50);
        verify(repository).findMatchFineDetail(10L, 1L);
    }

    @Test
    void playerDetailCombinesSeasonsOpponentsAndFinesAcrossHistory() {
        team.setId(1L);
        doReturn(List.of(
            playerRow(10L, 1L, " TJ A ", 5L, 20L),
            playerRow(11L, 2L, "TJ A", 6L, 30L),
            playerRow(12L, 3L, "TJ A", 5L, 40L),
            playerRow(13L, 1L, "TJ B", 5L, 50L),
            playerRow(14L, 1L, "TJ A", 7L, 60L))).when(repository).findPlayerFineDetail(2L, null, Config.ALL_SEASON_ID, 1L);
        StatisticsFilter filter = new StatisticsFilter();
        filter.setSeasonIds(List.of(1L, 2L));
        filter.setOpponentNames(List.of("tj a"));
        filter.setFineIds(List.of(5L, 6L));
        var response = getter.getPlayerDetail(2L, null, team, filter);
        assertThat(response.getMatches()).hasSize(2);
        assertThat(response.getFines()).hasSize(2);
        assertThat(response.getMatches().stream().mapToLong(m -> m.getTotalAmount()).sum()).isEqualTo(50);
    }

    @Test
    void unmatchedSelectionReturnsEmptyDetail() {
        team.setId(1L);
        doReturn(List.of(matchRow(1L, 5L, 20L))).when(repository).findMatchFineDetail(10L, 1L);
        StatisticsFilter filter = new StatisticsFilter();
        filter.setFineIds(List.of(99L));
        var response = getter.getMatchDetail(10L, team, filter);
        assertThat(response.getPlayers()).isEmpty();
        assertThat(response.getFines()).isEmpty();
    }

    private IMatchReceivedFineDetail matchRow(Long player, Long fine, Long amount) {
        var row = mock(IMatchReceivedFineDetail.class);
        when(row.getPlayerId()).thenReturn(player);
        when(row.getFineId()).thenReturn(fine);
        when(row.getFineCount()).thenReturn(1L);
        when(row.getTotalAmount()).thenReturn(amount);
        return row;
    }

    private IPlayerReceivedFineDetail playerRow(Long match, Long season, String name, Long fine, Long amount) {
        var row = mock(IPlayerReceivedFineDetail.class);
        when(row.getMatchId()).thenReturn(match);
        when(row.getSeasonId()).thenReturn(season);
        when(row.getMatchName()).thenReturn(name);
        when(row.getMatchDate()).thenReturn(new Date(match));
        when(row.getFineId()).thenReturn(fine);
        when(row.getFineCount()).thenReturn(1L);
        when(row.getTotalAmount()).thenReturn(amount);
        return row;
    }
}
