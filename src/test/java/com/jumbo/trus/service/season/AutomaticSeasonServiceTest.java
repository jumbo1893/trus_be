package com.jumbo.trus.service.season;

import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.jumbo.trus.config.Config.OTHER_SEASON_ID;

@ExtendWith(MockitoExtension.class)
class AutomaticSeasonServiceTest {
    @Mock AppTeamRepository appTeams;
    @Mock FootballMatchRepository footballMatches;
    @Mock SeasonRepository seasons;
    @Mock MatchRepository matches;
    @Mock OutboxEventService events;
    AutomaticSeasonService service;
    AppTeamEntity appTeam;

    @BeforeEach void setUp() {
        service = new AutomaticSeasonService(appTeams, footballMatches, seasons, matches, events);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-02-01T00:00:00Z"), ZoneOffset.UTC));
        TeamEntity footballTeam = new TeamEntity(); footballTeam.setId(8L);
        appTeam = new AppTeamEntity(); appTeam.setId(3L); appTeam.setTeam(footballTeam);
        lenient().when(appTeams.findAll()).thenReturn(List.of(appTeam));
        lenient().when(seasons.findByAppTeamIdAndAutomaticKey(anyLong(), anyString())).thenReturn(Optional.empty());
        lenient().when(seasons.findFirstByAppTeamIdAndName(anyLong(), anyString())).thenReturn(Optional.empty());
        lenient().when(seasons.saveAndFlush(any())).thenAnswer(invocation -> {
            SeasonEntity value = invocation.getArgument(0); if (value.getId() == null) value.setId(50L); return value;
        });
        lenient().when(matches.findMatchIdsBySeason(anyLong())).thenReturn(Set.of(101L));
    }

    @Test void createsSpringFromFirstAndLastImportedMatch() {
        when(footballMatches.findAllMatchDatesByTeamId(8L)).thenReturn(List.of(
                date("2026-05-20T18:00:00Z"), date("2026-03-10T18:00:00Z")));
        service.synchronizeAfterMatchImport();
        var captor = org.mockito.ArgumentCaptor.forClass(SeasonEntity.class);
        verify(seasons).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Jaro 2026");
        assertThat(captor.getValue().getAutomaticKey()).isEqualTo("SPRING_2026");
        assertThat(captor.getValue().getFromDate()).isEqualTo(date("2026-03-09T23:00:00Z"));
        assertThat(captor.getValue().getToDate()).isEqualTo(date("2026-05-20T21:59:59.999Z"));
    }

    @Test void createsFallbackAtMarchFirstWhenNoMatchWasImported() {
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC));
        when(footballMatches.findAllMatchDatesByTeamId(8L)).thenReturn(List.of());
        service.synchronizeAfterMatchImport();
        var captor = org.mockito.ArgumentCaptor.forClass(SeasonEntity.class);
        verify(seasons).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Jaro 2026");
        assertThat(captor.getValue().getFromDate().toInstant()).isEqualTo(Instant.parse("2026-02-28T23:00:00Z"));
        assertThat(captor.getValue().getToDate().toInstant()).isEqualTo(Instant.parse("2026-06-30T21:59:59.999Z"));
    }

    @Test void createsAutumnFallbackAtSeptemberFirst() {
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC));
        when(footballMatches.findAllMatchDatesByTeamId(8L)).thenReturn(List.of());

        service.synchronizeAfterMatchImport();

        var captor = org.mockito.ArgumentCaptor.forClass(SeasonEntity.class);
        verify(seasons, times(2)).saveAndFlush(captor.capture());
        SeasonEntity autumn = captor.getAllValues().stream()
                .filter(value -> value.getName().equals("Podzim 2026"))
                .findFirst().orElseThrow();
        assertThat(autumn.getFromDate().toInstant()).isEqualTo(Instant.parse("2026-08-31T22:00:00Z"));
        assertThat(autumn.getToDate().toInstant()).isEqualTo(Instant.parse("2026-12-31T22:59:59.999Z"));
    }

    @Test void doesNotCreateFallbackBeforeMarchFirst() {
        when(footballMatches.findAllMatchDatesByTeamId(8L)).thenReturn(List.of());

        service.synchronizeAfterMatchImport();

        verify(seasons, never()).saveAndFlush(any());
    }

    @Test void importedChangeUpdatesAutomaticDatesButNotManuallyLockedDates() {
        SeasonEntity season = new SeasonEntity(); season.setId(50L); season.setAppTeam(appTeam);
        season.setName("Jaro 2026"); season.setAutomaticKey("SPRING_2026");
        season.setFromDate(date("2026-03-10T18:00:00Z")); season.setToDate(date("2026-05-20T18:00:00Z"));
        when(seasons.findByAppTeamIdAndAutomaticKey(3L, "SPRING_2026")).thenReturn(Optional.of(season));
        when(footballMatches.findAllMatchDatesByTeamId(8L)).thenReturn(List.of(
                date("2026-03-01T18:00:00Z"), date("2026-06-01T18:00:00Z")));
        service.synchronizeAfterMatchImport();
        assertThat(season.getFromDate()).isEqualTo(date("2026-02-28T23:00:00Z"));
        assertThat(season.getToDate()).isEqualTo(date("2026-06-01T21:59:59.999Z"));
        season.setDatesManuallyEdited(true); season.setFromDate(date("2026-03-05T18:00:00Z"));
        service.synchronizeAfterMatchImport();
        assertThat(season.getFromDate()).isEqualTo(date("2026-03-05T18:00:00Z"));
        assertThat(season.getToDate()).isEqualTo(date("2026-06-01T21:59:59.999Z"));
        verify(matches).assignUnclassifiedMatchesToSeason(
                3L, 50L, OTHER_SEASON_ID,
                date("2026-03-05T18:00:00Z"), date("2026-06-01T21:59:59.999Z"));
    }

    @Test void adoptsExistingStandardSeasonWithoutChangingItsDates() {
        SeasonEntity existing = new SeasonEntity(); existing.setId(70L); existing.setAppTeam(appTeam);
        existing.setName("Jaro 2026");
        existing.setFromDate(date("2026-03-05T18:00:00Z"));
        existing.setToDate(date("2026-06-10T18:00:00Z"));
        when(seasons.findFirstByAppTeamIdAndName(3L, "Jaro 2026")).thenReturn(Optional.of(existing));
        when(footballMatches.findAllMatchDatesByTeamId(8L)).thenReturn(List.of(
                date("2026-03-01T18:00:00Z"), date("2026-06-20T18:00:00Z")));

        service.synchronizeAfterMatchImport();

        assertThat(existing.getAutomaticKey()).isEqualTo("SPRING_2026");
        assertThat(existing.isDatesManuallyEdited()).isTrue();
        assertThat(existing.getFromDate()).isEqualTo(date("2026-03-05T18:00:00Z"));
        assertThat(existing.getToDate()).isEqualTo(date("2026-06-10T18:00:00Z"));
    }

    private Date date(String value) { return Date.from(Instant.parse(value)); }
}
