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
