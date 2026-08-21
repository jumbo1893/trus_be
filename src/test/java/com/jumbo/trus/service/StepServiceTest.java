package com.jumbo.trus.service;

import com.jumbo.trus.dto.step.StepDailyDTO;
import com.jumbo.trus.dto.step.StepBackgroundSyncRequestDTO;
import com.jumbo.trus.dto.step.StepSyncItemDTO;
import com.jumbo.trus.dto.step.StepSyncRequestDTO;
import com.jumbo.trus.dto.step.StepLeaderboardDTO;
import com.jumbo.trus.dto.step.StepLeaderboardResponseDTO;
import com.jumbo.trus.dto.step.StepPeriod;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.StepSource;
import com.jumbo.trus.entity.StepConsentEntity;
import com.jumbo.trus.entity.StepUpdateEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventPayload;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.repository.StepUpdateRepository;
import com.jumbo.trus.repository.StepConsentRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StepServiceTest {

    private final StepUpdateRepository repository = mock(StepUpdateRepository.class);
    private final UserService userService = mock(UserService.class);
    private final StepConsentRepository consentRepository = mock(StepConsentRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final AppTeamService appTeamService = mock(AppTeamService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserTeamRoleRepository userTeamRoleRepository = mock(UserTeamRoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final OutboxEventService outboxEventService = mock(OutboxEventService.class);
    private final StepService service = new StepService(
            repository, consentRepository, matchRepository, userService, appTeamService,
            userRepository, userTeamRoleRepository, passwordEncoder, outboxEventService);

    @BeforeEach
    void allowStepSharing() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(3L);
        StepConsentEntity consent = new StepConsentEntity();
        consent.setEnabled(true);
        when(appTeamService.getCurrentAppTeamOrThrow()).thenReturn(appTeam);
        when(consentRepository.findByUserIdAndAppTeamId(anyLong(), eq(3L)))
                .thenReturn(Optional.of(consent));
        PlayerEntity player = new PlayerEntity();
        player.setId(70L);
        UserTeamRole role = new UserTeamRole();
        role.setPlayer(player);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(anyLong(), eq(3L)))
                .thenReturn(Optional.of(role));
        when(userTeamRoleRepository.findConsentingPlayerIdsByAppTeamId(3L))
                .thenReturn(List.of(70L));
        when(matchRepository.findIdsByAppTeamAndDateBetween(eq(3L), any(Date.class), any(Date.class)))
                .thenReturn(List.of());
        when(matchRepository.findFirstByAppTeamIdAndDateLessThanOrderByDateDesc(eq(3L), any(Date.class)))
                .thenReturn(Optional.empty());
    }

    @Test
    void synchronizesSeveralMissingDaysInOneRequest() {
        UserEntity user = user(7L);
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(repository.findByUserIdAndDate(eq(7L), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StepSyncRequestDTO request = request(
                item("2026-08-12", 4_000, "2026-08-12T21:00:00+02:00"),
                item("2026-08-13", 7_500, "2026-08-13T22:00:00+02:00")
        );

        List<StepDailyDTO> result = service.sync(request);

        assertEquals(2, result.size());
        assertEquals(4_000, result.get(0).stepCount());
        assertEquals(7_500, result.get(1).stepCount());
        verify(repository, times(2)).save(any(StepUpdateEntity.class));

        ArgumentCaptor<OutboxEventPayload> payloadCaptor = ArgumentCaptor.forClass(OutboxEventPayload.class);
        verify(outboxEventService).createEventForTeam(
                eq(OutboxEventType.STEP_SYNCED),
                eq(OutboxAggregateType.STEP),
                eq(7L),
                payloadCaptor.capture(),
                eq(3L),
                eq(7L));
        assertEquals(Set.of(70L), payloadCaptor.getValue().affectedPlayerIds());
    }

    @Test
    void ignoresAnOlderSnapshotSoRetryCannotRollStepsBack() {
        UserEntity user = user(7L);
        StepUpdateEntity existing = entity(user, 8_000, "2026-08-14T20:00:00+02:00");
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(repository.findByUserIdAndDate(7L, LocalDate.parse("2026-08-14"))).thenReturn(Optional.of(existing));

        List<StepDailyDTO> result = service.sync(request(
                item("2026-08-14", 6_000, "2026-08-14T18:00:00+02:00")));

        assertEquals(8_000, result.get(0).stepCount());
        verify(repository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void acceptsNewerCorrectedAggregationEvenWhenItIsLower() {
        UserEntity user = user(7L);
        StepUpdateEntity existing = entity(user, 8_000, "2026-08-14T20:00:00+02:00");
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(repository.findByUserIdAndDate(7L, LocalDate.parse("2026-08-14"))).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.sync(request(item("2026-08-14", 7_900, "2026-08-14T21:00:00+02:00")));

        ArgumentCaptor<StepUpdateEntity> captor = ArgumentCaptor.forClass(StepUpdateEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(7_900, captor.getValue().getStepNumber());
    }

    @Test
    void backgroundSyncRevokesConsentWhenHealthPermissionWasRemoved() {
        UserEntity user = user(7L);
        user.setMail("test@example.com");
        user.setPassword("encoded");
        StepConsentEntity consent = new StepConsentEntity();
        consent.setEnabled(true);
        when(userRepository.findByMail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("firebase-uid", "encoded")).thenReturn(true);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(7L, 3L))
                .thenReturn(Optional.of(new UserTeamRole()));
        when(consentRepository.findByUserIdAndAppTeamId(7L, 3L))
                .thenReturn(Optional.of(consent));

        service.backgroundSync(new StepBackgroundSyncRequestDTO(
                "test@example.com", "firebase-uid", 3L, false, List.of()));

        assertEquals(false, consent.isEnabled());
        verify(consentRepository).save(consent);
        verifyNoInteractions(repository);
    }

    @Test
    void leaderboardSinceLastMatchUsesLatestPlayedMatchForCurrentAppTeam() {
        LocalDate matchDate = LocalDate.now(ZoneId.of("Europe/Prague")).minusDays(3);
        MatchEntity match = match(11L, "FC Test", matchDate);
        when(matchRepository.findFirst2ByAppTeamIdAndDateLessThanEqualOrderByDateDesc(
                eq(3L), any(Date.class)))
                .thenReturn(List.of(match));
        when(repository.leaderboard(eq(3L), eq(matchDate), any(LocalDate.class)))
                .thenReturn(List.of(new StepLeaderboardDTO(7L, "Test", 12_345, 4, 3_086.25)));

        StepLeaderboardResponseDTO result = service.getLeaderboard(StepPeriod.SINCE_LAST_MATCH);

        assertEquals(matchDate, result.from());
        assertEquals(11L, result.lastMatch().matchId());
        assertEquals("FC Test", result.lastMatch().opponentName());
        assertEquals(matchDate, result.lastMatch().date());
        assertEquals(12_345, result.entries().get(0).stepCount());
        assertEquals(4, result.entries().get(0).dayCount());
        assertEquals(3_086.25, result.entries().get(0).averageStepsPerDay());
    }

    @Test
    void leaderboardBetweenMatchesUsesPenultimateAndLatestPlayedMatch() {
        LocalDate latestDate = LocalDate.now(ZoneId.of("Europe/Prague")).minusDays(2);
        LocalDate previousDate = latestDate.minusDays(7);
        MatchEntity latest = match(12L, "FC Poslední", latestDate);
        MatchEntity previous = match(11L, "FC Předchozí", previousDate);
        when(matchRepository.findFirst2ByAppTeamIdAndDateLessThanEqualOrderByDateDesc(
                eq(3L), any(Date.class)))
                .thenReturn(List.of(latest, previous));
        when(repository.leaderboard(3L, previousDate, latestDate))
                .thenReturn(List.of(new StepLeaderboardDTO(7L, "Test", 70_000, 8, 8_750.0)));

        StepLeaderboardResponseDTO result = service.getLeaderboard(StepPeriod.BETWEEN_MATCHES);

        assertEquals(previousDate, result.from());
        assertEquals(latestDate, result.to());
        assertEquals("FC Předchozí", result.previousMatch().opponentName());
        assertEquals("FC Poslední", result.lastMatch().opponentName());
        assertEquals(70_000, result.entries().get(0).stepCount());
    }

    @Test
    void leaderboardBetweenMatchesIsEmptyUntilTeamHasTwoPlayedMatches() {
        LocalDate latestDate = LocalDate.now(ZoneId.of("Europe/Prague")).minusDays(2);
        when(matchRepository.findFirst2ByAppTeamIdAndDateLessThanEqualOrderByDateDesc(
                eq(3L), any(Date.class)))
                .thenReturn(List.of(match(12L, "FC Poslední", latestDate)));

        StepLeaderboardResponseDTO result = service.getLeaderboard(StepPeriod.BETWEEN_MATCHES);

        assertNull(result.from());
        assertNull(result.to());
        assertNull(result.previousMatch());
        assertEquals("FC Poslední", result.lastMatch().opponentName());
        assertEquals(List.of(), result.entries());
        verifyNoInteractions(repository);
    }

    private static StepSyncRequestDTO request(StepSyncItemDTO... items) {
        StepSyncRequestDTO request = new StepSyncRequestDTO();
        request.setDays(List.of(items));
        return request;
    }

    private static StepSyncItemDTO item(String date, int steps, String measuredUntil) {
        StepSyncItemDTO item = new StepSyncItemDTO();
        item.setDate(LocalDate.parse(date));
        item.setStepCount(steps);
        item.setSource(StepSource.HEALTH_CONNECT);
        item.setTimezone("Europe/Prague");
        item.setMeasuredUntil(OffsetDateTime.parse(measuredUntil));
        return item;
    }

    private static UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        return user;
    }

    private static MatchEntity match(Long id, String opponentName, LocalDate date) {
        MatchEntity match = new MatchEntity();
        match.setId(id);
        match.setName(opponentName);
        match.setDate(Date.from(date.atTime(18, 30)
                .atZone(ZoneId.of("Europe/Prague")).toInstant()));
        return match;
    }

    private static StepUpdateEntity entity(UserEntity user, int steps, String measuredUntil) {
        StepUpdateEntity entity = new StepUpdateEntity();
        entity.setUser(user);
        entity.setDate(LocalDate.parse("2026-08-14"));
        entity.setStepNumber(steps);
        entity.setSource(StepSource.HEALTH_CONNECT);
        entity.setTimezone("Europe/Prague");
        entity.setMeasuredUntil(OffsetDateTime.parse(measuredUntil));
        entity.setUpdateTime(Instant.now());
        return entity;
    }
}
