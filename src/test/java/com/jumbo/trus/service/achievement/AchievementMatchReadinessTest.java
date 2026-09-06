package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.outbox.*;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class AchievementMatchReadinessTest {
    @Test void eventCalculationDoesNotSaveOrNotifyPrematureAward() throws Exception {
        var constructor = AchievementCalculator.class.getDeclaredConstructors()[0];
        var calculator = (AchievementCalculator) constructor.newInstance(Arrays.stream(constructor.getParameterTypes())
                .map(type -> mock(type)).toArray());
        var catalog = (com.jumbo.trus.repository.achievement.AchievementRepository) ReflectionTestUtils.getField(calculator, "achievementRepository");
        var mapper = (com.jumbo.trus.mapper.achievement.AchievementMapper) ReflectionTestUtils.getField(calculator, "achievementMapper");
        var repository = (com.jumbo.trus.repository.achievement.PlayerAchievementRepository) ReflectionTestUtils.getField(calculator, "playerAchievementRepository");
        var readiness = (AchievementMatchReadiness) ReflectionTestUtils.getField(calculator, "matchReadiness");
        var entity = new com.jumbo.trus.entity.achievement.AchievementEntity();
        entity.setCalculationScope(com.jumbo.trus.entity.achievement.AchievementCalculationScope.MATCH);
        var achievement = new com.jumbo.trus.dto.achievement.AchievementDTO(); achievement.setId(1L);
        achievement.setCode("AUTICKO"); achievement.setAchievementTypes(Set.of(OutboxAggregateType.GOAL));
        achievement.setCalculationScope(entity.getCalculationScope());
        when(catalog.findAll()).thenReturn(List.of(entity)); when(mapper.toDTO(entity)).thenReturn(achievement);
        var projection = mock(com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo.class);
        when(repository.findAutickoInMatch(7L, 3L)).thenReturn(projection);
        var matchService = (com.jumbo.trus.service.match.MatchService) ReflectionTestUtils.getField(calculator, "matchService");
        var match = new MatchDTO(); match.setId(3L); when(matchService.getMatch(3L)).thenReturn(match);
        when(readiness.deferIfPending(eq("AUTICKO"), any(), eq(1L))).thenReturn(true);
        var player = new com.jumbo.trus.dto.player.PlayerDTO(); player.setId(7L);
        var team = new com.jumbo.trus.entity.auth.AppTeamEntity(); team.setId(1L);
        calculator.calculateEventAchievements(List.of(player), team,
                Map.of(7L, new com.jumbo.trus.service.outbox.AchievementPlayerWork(Map.of(3L, Set.of(OutboxAggregateType.GOAL)), Set.of())), Set.of());
        verify(repository, never()).save(any());
        var notifications = (com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker) ReflectionTestUtils.getField(calculator, "achievementNotificationMaker");
        verify(notifications).sendAchievementNotify(List.of(), team);
        verifyNoMoreInteractions(notifications);
        verifyNoInteractions((com.jumbo.trus.service.membership.MembershipService) ReflectionTestUtils.getField(calculator, "membershipService"));
    }

    @Test void schedulesOneDurableRecalculationAndAllowsCalculationAfterCompletion() {
        var matches = mock(MatchRepository.class); var events = mock(OutboxEventRepository.class);
        var readiness = new AchievementMatchReadiness(matches, events);
        Instant start = Instant.parse("2026-09-06T10:00:00Z");
        ReflectionTestUtils.setField(readiness, "clock", Clock.fixed(start.plusSeconds(30), ZoneOffset.UTC));
        var match = new MatchEntity(); match.setId(3L); match.setDate(Date.from(start));
        var result = new PlayerAchievementDTO(); var dto = new MatchDTO(); dto.setId(3L); result.setMatch(dto);
        when(matches.findAwaitingFinalStatistics(eq(1L), eq(3L), isNull(), any(), any())).thenReturn(List.of(match));
        when(events.existsByAppTeamIdAndAggregateIdAndEventTypeAndStatusIn(eq(1L), eq(3L), any(), anyList()))
                .thenReturn(false, true);
        assertThat(readiness.deferIfPending("AUTICKO", result, 1L)).isTrue();
        assertThat(readiness.deferIfPending("AUTICKO", result, 1L)).isTrue();
        var captor = ArgumentCaptor.forClass(OutboxEventEntity.class); verify(events).save(captor.capture());
        assertThat(captor.getValue().getNextAttemptAt()).isEqualTo(start.plusSeconds(3600));
        assertThat(captor.getValue().getEventType()).isEqualTo(OutboxEventType.MATCH_ACHIEVEMENTS_READY);
        assertThat(captor.getValue().getAggregateType()).isEqualTo(OutboxAggregateType.ALL);
        assertThat(captor.getValue().getPayload().matchId()).isEqualTo(3L);
        when(matches.findAwaitingFinalStatistics(eq(1L), eq(3L), isNull(), any(), any())).thenReturn(List.of());
        assertThat(readiness.deferIfPending("AUTICKO", result, 1L)).isFalse();
    }

    @Test void stableMilestonesAreNotDelayed() {
        var matches = mock(MatchRepository.class); var events = mock(OutboxEventRepository.class);
        var readiness = new AchievementMatchReadiness(matches, events);
        assertThat(readiness.deferIfPending("NESOBECKY_HRDINA", new PlayerAchievementDTO(), 1L)).isFalse();
        verifyNoInteractions(matches, events);
    }
}
