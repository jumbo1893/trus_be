package com.jumbo.trus.service.goal;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.goal.projection.IGoalAttendanceDetail;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.repository.GoalRepository;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GoalDetailedStatsFilterTest {
    private final GoalRepository repository = mock(GoalRepository.class);
    private final GoalDetailedStatsService service = new GoalDetailedStatsService(repository);

    private IGoalAttendanceDetail row(long season, long player, long match, int goals) {
        var row = mock(IGoalAttendanceDetail.class);
        when(row.getSeasonId()).thenReturn(season);
        when(row.getPlayerId()).thenReturn(player);
        when(row.getPlayerName()).thenReturn("Hráč " + player);
        when(row.getMatchId()).thenReturn(match);
        when(row.getMatchName()).thenReturn("Soupeř");
        when(row.getMatchDate()).thenReturn(new Date(match));
        when(row.getGoalNumber()).thenReturn(goals);
        return row;
    }

    @Test
    void seasonSelectionFiltersPlayerAndMatchRowsAndTotals() {
        var team = new AppTeamEntity(); team.setId(7L);
        var filter = new StatisticsFilter(null, null, null, false, team);
        var rows = List.of(row(10, 1, 100, 2), row(11, 2, 101, 3), row(12, 3, 102, 90));
        when(repository.findGoalAttendanceDetails(7L, Config.ALL_SEASON_ID, null, null, null))
                .thenReturn(rows);
        filter.setSeasonIds(List.of(10L, 11L));
        for (boolean matches : List.of(false, true)) {
            filter.setMatchStatsOrPlayerStats(matches);
            var result = service.getAllDetailed(filter);
            assertThat(result.getGoalList()).hasSize(2);
            assertThat(result.getGoalList()).extracting(r -> r.getGoalNumber()).containsExactlyInAnyOrder(2, 3);
            assertThat(result.getPlayersCount()).isEqualTo(2);
            assertThat(result.getTotalGoals()).isEqualTo(5);
            assertThat(result.getMatchesCount()).isEqualTo(2);
        }
        filter.setSeasonIds(List.of(11L));
        assertThat(service.getAllDetailed(filter).getGoalList()).singleElement().satisfies(r -> assertThat(r.getGoalNumber()).isEqualTo(3));
        filter.setSeasonIds(List.of());
        assertThat(service.getAllDetailed(filter).getGoalList()).hasSize(3);
        filter.setSeasonIds(List.of(99L));
        assertThat(service.getAllDetailed(filter).getGoalList()).isEmpty();
    }
}
