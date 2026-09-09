package com.jumbo.trus.service;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.SeasonMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.service.achievement.SeasonAchievementTiming;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import com.jumbo.trus.entity.filter.SeasonFilter;
import static com.jumbo.trus.config.Config.OTHER_SEASON_ID;
import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeasonServiceTest {

    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final SeasonMapper seasonMapper = mock(SeasonMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final OutboxEventService outboxEventService = mock(OutboxEventService.class);
    private final SeasonService service = new SeasonService(
            seasonRepository,
            matchRepository,
            seasonMapper,
            notificationService,
            outboxEventService
    );

    @Test
    void playedFilterHidesEmptySeasonsButManagementKeepsThem() {
        var team = new AppTeamEntity(); team.setId(7L);
        var played = new SeasonEntity(); played.setId(10L);
        var empty = new SeasonEntity(); empty.setId(11L);
        var date = new Date();
        when(seasonRepository.getAllWithoutNonEditable(anyInt(), eq(7L))).thenReturn(List.of(played, empty));
        when(seasonMapper.toDTO(played)).thenReturn(new SeasonDTO(10L, "Odehraná", date, date));
        when(seasonMapper.toDTO(empty)).thenReturn(new SeasonDTO(11L, "Prázdná", date, date));
        when(matchRepository.findPlayedSeasonIds(eq(7L), any(Date.class))).thenReturn(Set.of(10L));
        var filter = new SeasonFilter(false, true, false);
        filter.setAppTeam(team);
        filter.setPlayedOnly(true);
        assertThat(service.getAll(filter)).extracting(SeasonDTO::getId).containsExactly(10L);
        when(matchRepository.findPlayedSeasonIds(eq(7L), any(Date.class))).thenReturn(Set.of(10L, OTHER_SEASON_ID));
        assertThat(service.getAll(filter)).extracting(SeasonDTO::getId).containsExactly(10L, OTHER_SEASON_ID);
        filter.setPlayedOnly(false);
        assertThat(service.getAll(filter)).extracting(SeasonDTO::getId).containsExactlyInAnyOrder(10L, 11L, OTHER_SEASON_ID);
    }

    @Test
    void currentSeasonIncludesTheWholeLastCalendarDay() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(7L);
        LocalDate today = LocalDate.now(SeasonAchievementTiming.ZONE);
        Date from = Date.from(today.minusMonths(1)
                .atStartOfDay(SeasonAchievementTiming.ZONE).toInstant());
        Date to = Date.from(today
                .atStartOfDay(SeasonAchievementTiming.ZONE).toInstant());

        SeasonEntity entity = new SeasonEntity();
        entity.setId(42L);
        entity.setName("Aktuální sezona");
        entity.setFromDate(from);
        entity.setToDate(to);
        SeasonDTO dto = new SeasonDTO(42L, "Aktuální sezona", from, to);

        when(seasonRepository.getAllWithoutNonEditable(anyInt(), eq(7L)))
                .thenReturn(List.of(entity));
        when(seasonMapper.toDTO(entity)).thenReturn(dto);

        assertThat(service.getCurrentSeason(true, appTeam).getId()).isEqualTo(42L);
    }
}
