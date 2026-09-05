package com.jumbo.trus.service;

import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import java.util.Date;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttendanceServiceTest {
    private final MatchRepository repository = mock(MatchRepository.class);
    private final MatchMapper matchMapper = mock(MatchMapper.class);
    private final PlayerMapper playerMapper = mock(PlayerMapper.class);
    private final AttendanceService service = new AttendanceService(repository, matchMapper, playerMapper);

    @Test
    void matchStatisticsCountOnlySelectedPlayersAndFansAndExcludeUnmatchedMatches() {
        var player = player(1L, false);
        var fan = player(2L, true);
        var other = player(3L, false);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(
            match(10L, List.of(player, fan, other)), match(11L, List.of(other))));
        when(matchMapper.toDTO(any())).thenAnswer(invocation -> {
            MatchEntity match = invocation.getArgument(0);
            MatchDTO dto = new MatchDTO();
            dto.setId(match.getId());
            dto.setDate(match.getDate());
            return dto;
        });
        StatisticsFilter filter = new StatisticsFilter();
        filter.setMatchStatsOrPlayerStats(true);
        filter.setPlayerIds(List.of(1L, 2L));
        var result = service.getAllDetailed(filter);
        assertThat(result.getMatchesCount()).isEqualTo(1);
        assertThat(result.getPlayersCount()).isEqualTo(2);
        assertThat(result.getAttendanceList()).singleElement().satisfies(row -> {
            assertThat(row.getPlayerCount()).isEqualTo(1);
            assertThat(row.getFanCount()).isEqualTo(1);
            assertThat(row.getTotalCount()).isEqualTo(2);
        });
    }

    @Test
    void selectedMatchDetailKeepsPlayerSelection() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(
            match(10L, List.of(player(1L, false), player(2L, true)))));
        StatisticsFilter filter = new StatisticsFilter();
        filter.setMatchId(10L);
        filter.setPlayerIds(List.of(2L));
        var result = service.getAllDetailed(filter);
        assertThat(result.getAttendanceList()).singleElement().satisfies(row ->
            assertThat(row.getId()).isEqualTo(2L));
        assertThat(result.getPlayersCount()).isEqualTo(1);
    }

    private PlayerEntity player(Long id, boolean fan) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setFan(fan);
        player.setName("Player " + id);
        return player;
    }

    private MatchEntity match(Long id, List<PlayerEntity> players) {
        MatchEntity match = new MatchEntity();
        match.setId(id);
        match.setDate(new Date(id));
        match.setPlayerList(players);
        return match;
    }
}
