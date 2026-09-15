package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.*;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.*;
import com.jumbo.trus.repository.achievement.*;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.membership.MembershipService;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AchievementBackfillTest {
    AchievementCalculator calculator;
    PlayerAchievementRepository repository;
    AchievementNotificationMaker notifications;
    PlayerDTO player;
    AppTeamEntity team;
    AchievementDTO definition;

    @BeforeEach
    void setup() throws Exception {
        var constructor = AchievementCalculator.class.getDeclaredConstructors()[0];
        calculator = (AchievementCalculator) constructor.newInstance(Arrays.stream(constructor.getParameterTypes())
                .map(type -> mock(type)).toArray());
        repository = dependency("playerAchievementRepository");
        notifications = dependency("achievementNotificationMaker");
        AchievementRepository definitions = dependency("achievementRepository");
        AchievementMapper mapper = dependency("achievementMapper");
        var entity = new AchievementEntity();
        entity.setCode("STRELKY");
        definition = new AchievementDTO();
        definition.setId(10); definition.setCode("STRELKY"); definition.setOnlyForPlayers(true);
        definition.setCalculationScope(AchievementCalculationScope.MATCH);
        when(definitions.findAll()).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(definition);
        player = new PlayerDTO(); player.setId(1); player.setActive(true);
        team = new AppTeamEntity(); team.setId(2L);
        var fact = mock(IMatchIdNumberOneNumberTwo.class);
        when(fact.getMatchId()).thenReturn(3L);
        when(fact.getFirstNumber()).thenReturn(2);
        when(fact.getSecondNumber()).thenReturn(1);
        when(repository.findStrelky(1L, 2L, null)).thenReturn(fact);
        MatchService matches = dependency("matchService");
        var match = new MatchDTO(); match.setId(3);
        when(matches.getMatch(3L)).thenReturn(match);
    }

    @SuppressWarnings("unchecked")
    private <T> T dependency(String name) { return (T) ReflectionTestUtils.getField(calculator, name); }

    @Test
    void previewDoesNotSaveNotifyOrCreditMembership() {
        var awards = calculator.backfillAwards(List.of(player), team, Set.of("STRELKY"), true);
        assertThat(awards).hasSize(1);
        assertThat(awards.get(0).getDetail()).contains("gólů: 2", "asistencí: 1");
        verify(repository, never()).save(any());
        MembershipService membership = dependency("membershipService");
        verifyNoInteractions(notifications, membership);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void newAwardIsSavedAndNotifiedOnlyOnce(boolean hasUnaccomplishedRow) {
        PlayerAchievementMapper mapper = dependency("playerAchievementMapper");
        var saved = new PlayerAchievementEntity(); saved.setId(77L);
        var owner = new PlayerEntity(); owner.setId(1L); saved.setPlayer(owner);
        var dto = new PlayerAchievementDTO(); dto.setId(77); dto.setPlayer(player);
        dto.setAchievement(definition); dto.setAccomplished(true);
        when(mapper.toEntity(any())).thenReturn(saved);
        when(repository.save(saved)).thenReturn(saved);
        when(mapper.toDTO(saved)).thenReturn(dto);
        if (hasUnaccomplishedRow) {
            var oldEntity = new PlayerAchievementEntity();
            var oldDto = new PlayerAchievementDTO(); oldDto.setId(77); oldDto.setPlayer(player);
            oldDto.setAchievement(definition); oldDto.setAccomplished(false);
            when(repository.findAllByPlayerIdIn(any())).thenReturn(List.of(oldEntity));
            when(mapper.toDTO(oldEntity)).thenReturn(oldDto);
        }
        assertThat(calculator.backfillAwards(List.of(player), team, Set.of("STRELKY"), false)).containsExactly(dto);
        when(repository.findAllByPlayerIdIn(any())).thenReturn(List.of(saved));
        assertThat(calculator.backfillAwards(List.of(player), team, Set.of("STRELKY"), false)).isEmpty();
        verify(repository, times(1)).save(saved);
        verify(notifications, times(1)).sendAchievementNotify(List.of(dto), team);
        MembershipService membership = dependency("membershipService");
        verify(membership, times(1)).achievementAccomplished(1L, 77L);
    }

    @Test
    void fanAndUnknownCodeDoNotReceiveAward() {
        player.setFan(true);
        assertThat(calculator.backfillAwards(List.of(player), team, Set.of("STRELKY"), false)).isEmpty();
        assertThatThrownBy(() -> calculator.backfillAwards(List.of(player), team, Set.of("UNKNOWN"), false))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(notifications);
    }
}
