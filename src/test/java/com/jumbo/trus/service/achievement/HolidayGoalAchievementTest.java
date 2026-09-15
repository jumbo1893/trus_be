package com.jumbo.trus.service.achievement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.achievement.*;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.calendar.CzechCelebrationCalendar;
import com.jumbo.trus.service.achievement.helper.*;
import com.jumbo.trus.service.match.MatchService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HolidayGoalAchievementTest {
    AchievementCalculator calculator;
    PlayerAchievementRepository repository;
    PlayerDTO player;
    AchievementDTO definition;
    AppTeamEntity team;

    @BeforeEach
    void setup() throws Exception {
        var constructor = AchievementCalculator.class.getDeclaredConstructors()[0];
        calculator = (AchievementCalculator) constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(t -> mock(t)).toArray());
        ReflectionTestUtils.setField(calculator, "celebrationCalendar", new CzechCelebrationCalendar(new ObjectMapper()));
        repository = (PlayerAchievementRepository) ReflectionTestUtils.getField(calculator, "playerAchievementRepository");
        MatchService matches = (MatchService) ReflectionTestUtils.getField(calculator, "matchService");
        when(matches.getMatch(anyLong())).thenAnswer(invocation -> {
            var match = new MatchDTO(); match.setId(invocation.getArgument(0)); return match;
        });
        player = new PlayerDTO(); player.setId(1); player.setName("Jan");
        var footballPlayer = new FootballPlayerDTO(); footballPlayer.setName("Novák Jan"); player.setFootballPlayer(footballPlayer);
        team = new AppTeamEntity(); team.setId(2L);
        definition = new AchievementDTO(); definition.setCode("SVATECNI_STRELEC"); definition.setOnlyForPlayers(true);
        definition.setCalculationScope(com.jumbo.trus.entity.achievement.AchievementCalculationScope.MATCH);
    }

    private IHolidayGoalMatch fact(long id, String instant, int goals) {
        return new IHolidayGoalMatch() {
            public Long getMatchId() { return id; }
            public Date getMatchDate() { return Date.from(Instant.parse(instant)); }
            public Integer getGoals() { return goals; }
        };
    }

    private PlayerAchievementDTO calculate(Long id) {
        return ReflectionTestUtils.invokeMethod(calculator, "calculateSvatecniStrelec", player, definition, team, AchievementType.ALL, id);
    }

    @Test
    void scopedCalculationUsesPragueDateAndShowsGoalsAndMatch() {
        when(repository.findHolidayGoalCandidates(1L, 2L, 3L)).thenReturn(List.of(fact(3, "2026-06-23T22:30:00Z", 2)));
        var result = calculate(3L);
        assertThat(result.getAccomplished()).isTrue();
        assertThat(result.getMatch().getId()).isEqualTo(3);
        assertThat(result.getDetail()).isEqualTo("Jmeniny: Jan. Počet gólů: 2.");
    }

    @Test
    void fullCalculationFindsFirstQualifyingHistoricalMatch() {
        when(repository.findHolidayGoalCandidates(1L, 2L, null)).thenReturn(List.of(
                fact(3, "2020-04-09T12:00:00Z", 3), fact(4, "2020-04-10T12:00:00Z", 1)));
        var result = calculate(null);
        assertThat(result.getMatch().getId()).isEqualTo(4);
        assertThat(result.getDetail()).contains("Velký pátek", "Počet gólů: 1");
    }

    @Test
    void missingLinkCannotUseNicknameButNationalHolidayStillQualifies() {
        player.setFootballPlayer(null);
        when(repository.findHolidayGoalCandidates(1L, 2L, 3L)).thenReturn(List.of(fact(3, "2026-06-24T12:00:00Z", 1)));
        assertThat(calculate(3L).getAccomplished()).isFalse();
        when(repository.findHolidayGoalCandidates(1L, 2L, 3L)).thenReturn(List.of(fact(3, "2026-05-01T12:00:00Z", 1)));
        assertThat(calculate(3L).getDetail()).contains("Svátek práce");
    }

    @Test
    void backfillUsesTheRegisteredHolidayCalculator() {
        var definitions = (com.jumbo.trus.repository.achievement.AchievementRepository) ReflectionTestUtils.getField(calculator, "achievementRepository");
        var mapper = (com.jumbo.trus.mapper.achievement.AchievementMapper) ReflectionTestUtils.getField(calculator, "achievementMapper");
        var entity = new com.jumbo.trus.entity.achievement.AchievementEntity(); entity.setCode("SVATECNI_STRELEC");
        when(definitions.findAll()).thenReturn(List.of(entity)); when(mapper.toDTO(entity)).thenReturn(definition);
        when(repository.findHolidayGoalCandidates(1L, 2L, null)).thenReturn(List.of(fact(3, "2026-06-24T12:00:00Z", 2)));
        var awards = calculator.backfillAwards(List.of(player), team, Set.of("SVATECNI_STRELEC"), true);
        assertThat(awards).hasSize(1);
        assertThat(awards.get(0).getDetail()).contains("Jmeniny: Jan", "Počet gólů: 2");
        verify(repository, never()).save(any());
    }

    @Test
    void fansAndZeroGoalsDoNotQualify() {
        when(repository.findHolidayGoalCandidates(1L, 2L, 3L)).thenReturn(List.of(fact(3, "2026-06-24T12:00:00Z", 0)));
        assertThat(calculate(3L).getAccomplished()).isFalse();
        player.setFan(true);
        assertThat(calculate(3L).getAccomplished()).isFalse();
    }
}
