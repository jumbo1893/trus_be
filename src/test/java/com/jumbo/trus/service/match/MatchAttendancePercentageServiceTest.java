package com.jumbo.trus.service.match;

import com.jumbo.trus.repository.MatchRepository;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchAttendancePercentageServiceTest {
    @Test
    void roundsAttendanceSinceFirstVisitForPlayersAndFans() {
        MatchRepository repository = mock(MatchRepository.class);
        MatchRepository.AttendancePercentageRow player = row(1L, 3L, 4L);
        MatchRepository.AttendancePercentageRow fan = row(2L, 1L, 3L);
        when(repository.findAttendancePercentages(eq(7L), any(Date.class)))
                .thenReturn(List.of(player, fan));

        var percentages = new MatchAttendancePercentageService(repository)
                .getPercentages(7L);

        assertThat(percentages).containsEntry(1L, 75).containsEntry(2L, 33);
    }

    private MatchRepository.AttendancePercentageRow row(
            Long playerId, Long attended, Long eligible) {
        MatchRepository.AttendancePercentageRow row =
                mock(MatchRepository.AttendancePercentageRow.class);
        when(row.getPlayerId()).thenReturn(playerId);
        when(row.getAttendedMatches()).thenReturn(attended);
        when(row.getEligibleMatches()).thenReturn(eligible);
        return row;
    }
}
