package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.achievement.*;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.service.achievement.AchievementAwardAuditService;
import com.jumbo.trus.service.achievement.AchievementCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class AchievementAwardAuditPersistenceTest extends AchievementDatabaseFixture {
    @Autowired MatchRepository matches;

    @Test void auditQueryIsTeamScopedAndRepairPersistsWithoutSaveNotificationFlow() {
        // Use an existing catalog definition when available; the fixture owns the player.
        AchievementEntity achievement = em.createQuery("select a from AchievementEntity a", AchievementEntity.class)
                .setMaxResults(1).getResultStream().findFirst().orElseGet(() -> {
                    var a = new AchievementEntity(); a.setCode("AUDIT_TEST"); a.setName("Audit test");
                    a.setCalculationScope(AchievementCalculationScope.MATCH);
                    return save(a);
                });
        var row = save(new PlayerAchievementEntity(achievement, player, true, new Date()));
        var mapper = mock(PlayerAchievementMapper.class);
        var pureCalculator = mock(AchievementCalculator.class);
        var input = new PlayerAchievementDTO();
        when(mapper.toDTO(any())).thenReturn(input);
        var failed = new PlayerAchievementDTO(); failed.setAccomplished(false);
        when(pureCalculator.auditAward(eq(input), eq(team), anyList())).thenReturn(failed);
        var service = new AchievementAwardAuditService(repository, mapper, pureCalculator, matches);
        assertThat(repository.findAwardAuditBatch(-999L, 0, PageRequest.of(0, 10))).isEmpty();
        var preview = service.audit(team, true, 0, 10, Set.of());
        assertThat(preview.entries()).hasSize(1);
        em.flush(); em.clear();
        assertThat(em.find(PlayerAchievementEntity.class, row.getId()).getAccomplished()).isTrue();
        service.audit(team, false, 0, 10, Set.of());
        em.flush(); em.clear();
        assertThat(em.find(PlayerAchievementEntity.class, row.getId()).getAccomplished()).isFalse();
        assertThat(service.audit(team, false, 0, 10, Set.of()).entries()).isEmpty();
    }
}
