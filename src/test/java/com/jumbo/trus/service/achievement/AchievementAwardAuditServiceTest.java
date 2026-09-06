package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.achievement.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class AchievementAwardAuditServiceTest {
    @Test void cursorAdvancesOverExcludedCodesWithoutCalculatingTheNextPage() {
        var repo = mock(PlayerAchievementRepository.class);
        var mapper = mock(PlayerAchievementMapper.class);
        var calculator = mock(AchievementCalculator.class);
        var matches = mock(MatchRepository.class);
        var team = new AppTeamEntity(); team.setId(1L);
        var achievement = new AchievementEntity(); achievement.setCode("AUTICKO");
        var first = new PlayerAchievementEntity(); first.setId(10L); first.setAchievement(achievement);
        var next = new PlayerAchievementEntity(); next.setId(11L); next.setAchievement(achievement);
        when(repo.findAwardAuditBatch(eq(1L), eq(0L), any())).thenReturn(List.of(first, next));
        var service = new AchievementAwardAuditService(repo, mapper, calculator, matches);
        var result = service.audit(team, false, 0, 1, Set.of("MECENAS"));
        assertThat(result.hasMore()).isTrue(); assertThat(result.nextAfterId()).isEqualTo(10L);
        assertThat(result.entries()).isEmpty();
        verifyNoInteractions(mapper, calculator, matches);
    }

    @Test void dryRunDoesNotChangeAwardAndRepairClearsInvalidAwardSilently() {
        var repo = mock(PlayerAchievementRepository.class);
        var mapper = mock(PlayerAchievementMapper.class);
        var calculator = mock(AchievementCalculator.class);
        var matches = mock(MatchRepository.class);
        var service = new AchievementAwardAuditService(repo, mapper, calculator, matches);
        var team = new AppTeamEntity(); team.setId(1L);
        var player = new PlayerEntity(); player.setId(2L);
        var achievement = new AchievementEntity(); achievement.setCode("MECENAS");
        var row = new PlayerAchievementEntity(); row.setId(3L); row.setPlayer(player);
        row.setAchievement(achievement); row.setAccomplished(true); row.setSeasonId(4L);
        row.setMatch(new MatchEntity()); row.setDetail("Original"); row.setAccomplishedDate(new Date());
        when(repo.findAwardAuditBatch(eq(1L), eq(0L), any())).thenReturn(List.of(row));
        when(matches.findCompletedMatchIds(eq(1L), any())).thenReturn(List.of());
        var input = new PlayerAchievementDTO(); when(mapper.toDTO(row)).thenReturn(input);
        var failed = new PlayerAchievementDTO(); failed.setAccomplished(false);
        when(calculator.auditAward(input, team, List.of())).thenReturn(failed);
        var preview = service.audit(team, true, 0, 10, Set.of());
        assertThat(preview.entries().get(0).outcome()).isEqualTo("REVOKED");
        assertThat(row.getAccomplished()).isTrue(); assertThat(row.getDetail()).isEqualTo("Original");
        var result = service.audit(team, false, 0, 10, Set.of());
        assertThat(result.nextAfterId()).isEqualTo(3L); assertThat(result.hasMore()).isFalse();
        assertThat(row.getAccomplished()).isFalse(); assertThat(row.getDetail()).isNull();
        assertThat(row.getSeasonId()).isNull(); assertThat(row.getMatch()).isNull();
        assertThat(row.getAccomplishedDate()).isNull();
        verify(calculator, times(2)).auditAward(input, team, List.of());
        verifyNoMoreInteractions(calculator);
    }
}
