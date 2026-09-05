package com.jumbo.trus.service;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.SeasonMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonServiceAutomaticDateLockTest {
    @Mock SeasonRepository seasons;
    @Mock MatchRepository matches;
    @Mock SeasonMapper mapper;
    @Mock NotificationService notifications;
    @Mock OutboxEventService events;
    SeasonService service;
    SeasonEntity entity;
    AppTeamEntity team;

    @BeforeEach void setUp() {
        service = new SeasonService(seasons, matches, mapper, notifications, events);
        team = new AppTeamEntity(); team.setId(3L);
        entity = new SeasonEntity(); entity.setId(50L); entity.setAppTeam(team); entity.setName("Jaro 2026");
        entity.setFromDate(date("2026-03-10T18:00:00Z")); entity.setToDate(date("2026-05-20T18:00:00Z"));
        when(seasons.findByIdAndAppTeamId(50L, 3L)).thenReturn(Optional.of(entity));
        when(matches.findMatchIdsBySeason(50L)).thenReturn(Set.of());
        when(seasons.getAllWithoutNonEditable(anyInt(), eq(3L))).thenReturn(List.of());
        when(mapper.toDTO(any())).thenAnswer(i -> {
            SeasonEntity value = i.getArgument(0);
            return new SeasonDTO(value.getId(), value.getName(), value.getFromDate(), value.getToDate());
        });
        when(seasons.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void changingEitherDatePermanentlyLocksAutomaticBoundaries() {
        service.editSeason(50L, new SeasonDTO(50L, "Jaro 2026",
                date("2026-03-01T18:00:00Z"), entity.getToDate()), team);
        assertThat(entity.isDatesManuallyEdited()).isTrue();
        assertThat(entity.getAchievementEventForEnd()).isNull();
    }

    @Test void changingOnlyNameDoesNotLockDates() {
        service.editSeason(50L, new SeasonDTO(50L, "Vlastní název",
                entity.getFromDate(), entity.getToDate()), team);
        assertThat(entity.isDatesManuallyEdited()).isFalse();
    }

    private Date date(String value) { return Date.from(Instant.parse(value)); }
}
