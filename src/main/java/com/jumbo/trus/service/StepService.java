package com.jumbo.trus.service;

import com.jumbo.trus.dto.step.StepDailyDTO;
import com.jumbo.trus.dto.step.StepConsentDTO;
import com.jumbo.trus.dto.step.StepBackgroundSyncRequestDTO;
import com.jumbo.trus.dto.step.StepLeaderboardDTO;
import com.jumbo.trus.dto.step.StepLeaderboardResponseDTO;
import com.jumbo.trus.dto.step.StepHistoryDayDTO;
import com.jumbo.trus.dto.step.StepHistoryResponseDTO;
import com.jumbo.trus.dto.step.StepMatchDTO;
import com.jumbo.trus.dto.step.StepPeriod;
import com.jumbo.trus.dto.step.StepSyncItemDTO;
import com.jumbo.trus.dto.step.StepSyncRequestDTO;
import com.jumbo.trus.entity.StepUpdateEntity;
import com.jumbo.trus.entity.StepConsentEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.repository.StepUpdateRepository;
import com.jumbo.trus.repository.StepConsentRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.exceptions.StepValidationException;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StepService {

    private static final int DEFAULT_HISTORY_DAYS = 7;
    private static final int MAX_RANGE_DAYS = 366;

    private final StepUpdateRepository stepUpdateRepository;
    private final StepConsentRepository stepConsentRepository;
    private final MatchRepository matchRepository;
    private final UserService userService;
    private final AppTeamService appTeamService;
    private final UserRepository userRepository;
    private final UserTeamRoleRepository userTeamRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxEventService outboxEventService;

    @Transactional
    public List<StepDailyDTO> sync(StepSyncRequestDTO request) {
        UserEntity user = userService.getCurrentUserEntity();
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        boolean consentEnabled = stepConsentRepository
                .findByUserIdAndAppTeamId(user.getId(), appTeam.getId())
                .map(StepConsentEntity::isEnabled)
                .orElse(false);
        if (!consentEnabled) {
            throw new StepValidationException("Pro synchronizaci kroků je nutný souhlas uživatele");
        }
        UserTeamRole role = userTeamRoleRepository
                .findByUserIdAndAppTeamId(user.getId(), appTeam.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        List<StepUpsertResult> results = request.getDays().stream()
                .map(item -> upsert(user, item))
                .toList();
        publishStepEvent(user, appTeam.getId(), role, results);
        return results.stream().map(StepUpsertResult::entity).map(this::toDTO).toList();
    }

    @Transactional
    public List<StepDailyDTO> backgroundSync(StepBackgroundSyncRequestDTO request) {
        UserEntity user = resolveBackgroundSyncUser(request);
        UserTeamRole role = userTeamRoleRepository.findByUserIdAndAppTeamId(user.getId(), request.appTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        StepConsentEntity consent = stepConsentRepository
                .findByUserIdAndAppTeamId(user.getId(), request.appTeamId())
                .orElseThrow(() -> new StepValidationException("Souhlas s kroky nebyl udělen"));
        if (!request.permissionGranted()) {
            consent.setEnabled(false);
            consent.setUpdatedAt(Instant.now());
            stepConsentRepository.save(consent);
            return List.of();
        }
        if (!consent.isEnabled()) {
            throw new StepValidationException("Souhlas s kroky nebyl udělen");
        }
        List<StepUpsertResult> results = request.days().stream()
                .map(item -> upsert(user, item))
                .toList();
        publishStepEvent(user, request.appTeamId(), role, results);
        return results.stream().map(StepUpsertResult::entity).map(this::toDTO).toList();
    }

    private UserEntity resolveBackgroundSyncUser(StepBackgroundSyncRequestDTO request) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity user) {
            return user;
        }
        if (request.mail() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByMail(request.mail().toLowerCase().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @Transactional(readOnly = true)
    public List<StepDailyDTO> getMySteps(LocalDate from, LocalDate to) {
        UserEntity user = userService.getCurrentUserEntity();
        return getStepsForUser(user.getId(), from, to);
    }

    @Transactional(readOnly = true)
    public List<StepDailyDTO> getStepsForUser(Long userId, LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(DEFAULT_HISTORY_DAYS - 1L) : from;
        validateRange(effectiveFrom, effectiveTo);
        return stepUpdateRepository
                .findAllByUserIdAndDateBetweenOrderByDateAsc(userId, effectiveFrom, effectiveTo)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public StepHistoryResponseDTO getHistory(Long requestedUserId, int days) {
        if (days < 1 || days > MAX_RANGE_DAYS) {
            throw new StepValidationException("Historii lze načíst v rozsahu 1 až 366 dní");
        }

        UserEntity currentUser = userService.getCurrentUserEntity();
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        requireEnabledConsent(currentUser.getId(), appTeam.getId());

        Long targetUserId = requestedUserId == null ? currentUser.getId() : requestedUserId;
        UserTeamRole targetRole = userTeamRoleRepository
                .findByUserIdAndAppTeamId(targetUserId, appTeam.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!targetUserId.equals(currentUser.getId())) {
            requireEnabledConsent(targetUserId, appTeam.getId());
        }

        LocalDate to = LocalDate.now(ZoneId.of("Europe/Prague"));
        LocalDate from = to.minusDays(days - 1L);
        Map<LocalDate, StepUpdateEntity> updatesByDate = new HashMap<>();
        stepUpdateRepository
                .findAllByUserIdAndDateBetweenOrderByDateAsc(targetUserId, from, to)
                .forEach(update -> updatesByDate.put(update.getDate(), update));

        List<StepHistoryDayDTO> history = java.util.stream.IntStream
                .range(0, days)
                .mapToObj(offset -> to.minusDays(offset))
                .map(date -> {
                    StepUpdateEntity update = updatesByDate.get(date);
                    return new StepHistoryDayDTO(
                            date,
                            update == null ? null : update.getStepNumber());
                })
                .toList();

        String userName = targetRole.getUser().getName();
        if (userName == null || userName.isBlank()) {
            userName = "Uživatel";
        }
        return new StepHistoryResponseDTO(
                targetUserId,
                userName,
                from,
                to,
                history);
    }

    private void requireEnabledConsent(Long userId, Long appTeamId) {
        boolean enabled = stepConsentRepository
                .findByUserIdAndAppTeamId(userId, appTeamId)
                .map(StepConsentEntity::isEnabled)
                .orElse(false);
        if (!enabled) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Historie kroků je dostupná pouze uživatelům se souhlasem");
        }
    }

    @Transactional(readOnly = true)
    public StepConsentDTO getConsent() {
        UserEntity user = userService.getCurrentUserEntity();
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        boolean enabled = stepConsentRepository.findByUserIdAndAppTeamId(user.getId(), appTeam.getId())
                .map(StepConsentEntity::isEnabled)
                .orElse(false);
        return new StepConsentDTO(enabled);
    }

    @Transactional
    public StepConsentDTO setConsent(StepConsentDTO request) {
        UserEntity user = userService.getCurrentUserEntity();
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        StepConsentEntity consent = stepConsentRepository
                .findByUserIdAndAppTeamId(user.getId(), appTeam.getId())
                .orElseGet(StepConsentEntity::new);
        consent.setUser(user);
        consent.setAppTeam(appTeam);
        consent.setEnabled(request.enabled());
        consent.setUpdatedAt(Instant.now());
        stepConsentRepository.save(consent);
        return new StepConsentDTO(consent.isEnabled());
    }

    @Transactional(readOnly = true)
    public StepLeaderboardResponseDTO getLeaderboard(StepPeriod period) {
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        return getLeaderboardForTeam(period, appTeam);
    }

    @Transactional(readOnly = true)
    public StepLeaderboardResponseDTO getLeaderboardForTeam(StepPeriod period, AppTeamEntity appTeam) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Prague"));
        List<MatchEntity> matches = findLastMatches(appTeam.getId());
        MatchEntity lastMatch = matches.isEmpty() ? null : matches.get(0);
        MatchEntity previousMatch = matches.size() < 2 ? null : matches.get(1);
        StepMatchDTO lastMatchDTO = toMatchDTO(lastMatch);
        StepMatchDTO previousMatchDTO = toMatchDTO(previousMatch);

        if (period == StepPeriod.BETWEEN_MATCHES && previousMatch == null) {
            return new StepLeaderboardResponseDTO(
                    period, null, null, previousMatchDTO, lastMatchDTO, List.of());
        }

        LocalDate from;
        LocalDate to;
        switch (period) {
            case TODAY -> {
                from = today;
                to = today;
            }
            case BETWEEN_MATCHES -> {
                from = toPragueDate(previousMatch);
                to = toPragueDate(lastMatch);
            }
            case SINCE_LAST_MATCH -> {
                from = lastMatch == null ? today : toPragueDate(lastMatch);
                to = today;
            }
            case ALL_TIME -> {
                from = LocalDate.of(1970, 1, 1);
                to = today;
            }
            default -> throw new IllegalStateException("Nepodporované období kroků: " + period);
        }
        List<StepLeaderboardDTO> entries = stepUpdateRepository
                .leaderboard(appTeam.getId(), from, to);
        return new StepLeaderboardResponseDTO(
                period, from, to, previousMatchDTO, lastMatchDTO, entries);
    }

    private List<MatchEntity> findLastMatches(Long appTeamId) {
        return matchRepository
                .findFirst2ByAppTeamIdAndDateLessThanEqualOrderByDateDesc(appTeamId, new Date());
    }

    private StepMatchDTO toMatchDTO(MatchEntity match) {
        if (match == null) {
            return null;
        }
        return new StepMatchDTO(match.getId(), match.getName(), toPragueDate(match));
    }

    private LocalDate toPragueDate(MatchEntity match) {
        return match.getDate().toInstant()
                .atZone(ZoneId.of("Europe/Prague"))
                .toLocalDate();
    }

    private StepUpsertResult upsert(UserEntity user, StepSyncItemDTO item) {
        validateItem(item);
        StepUpdateEntity entity = stepUpdateRepository
                .findByUserIdAndDate(user.getId(), item.getDate())
                .orElseGet(() -> newEntity(user, item.getDate()));

        // Ignore an older snapshot. A newer snapshot may lower the value because
        // HealthKit/Health Connect can retrospectively correct their aggregation.
        if (entity.getMeasuredUntil() != null
                && item.getMeasuredUntil().isBefore(entity.getMeasuredUntil())) {
            return new StepUpsertResult(entity, false);
        }
        if (entity.getMeasuredUntil() != null
                && item.getMeasuredUntil().isEqual(entity.getMeasuredUntil())
                && item.getStepCount() <= entity.getStepNumber()) {
            return new StepUpsertResult(entity, false);
        }

        entity.setStepNumber(item.getStepCount());
        entity.setSource(item.getSource());
        entity.setTimezone(item.getTimezone());
        entity.setMeasuredUntil(item.getMeasuredUntil());
        entity.setUpdateTime(Instant.now());
        return new StepUpsertResult(stepUpdateRepository.save(entity), true);
    }

    private void publishStepEvent(
            UserEntity user,
            Long appTeamId,
            UserTeamRole role,
            List<StepUpsertResult> results
    ) {
        if (role.getPlayer() == null) {
            return;
        }
        List<LocalDate> changedDates = results.stream()
                .filter(StepUpsertResult::changed)
                .map(result -> result.entity().getDate())
                .toList();
        if (changedDates.isEmpty()) {
            return;
        }

        LocalDate earliestChangedDate = changedDates.stream().min(LocalDate::compareTo).orElseThrow();
        Instant now = Instant.now();
        Date startOfChangedRange = Date.from(earliestChangedDate.atStartOfDay(ZoneId.of("Europe/Prague")).toInstant());
        Set<Long> affectedMatchIds = new LinkedHashSet<>(
                matchRepository.findIdsByAppTeamAndDateBetween(appTeamId, startOfChangedRange, Date.from(now)));

        Date startOfToday = Date.from(LocalDate.now(ZoneId.of("Europe/Prague"))
                .atStartOfDay(ZoneId.of("Europe/Prague"))
                .toInstant());
        matchRepository.findFirstByAppTeamIdAndDateLessThanOrderByDateDesc(appTeamId, startOfToday)
                .map(MatchEntity::getId)
                .ifPresent(affectedMatchIds::add);

        Set<Long> affectedPlayerIds = new LinkedHashSet<>(
                userTeamRoleRepository.findConsentingPlayerIdsByAppTeamId(appTeamId));
        affectedPlayerIds.add(role.getPlayer().getId());

        outboxEventService.createEventForTeam(
                OutboxEventType.STEP_SYNCED,
                OutboxAggregateType.STEP,
                user.getId(),
                OutboxEventPayloadFactory.stepsUpdated(affectedPlayerIds, affectedMatchIds),
                appTeamId,
                user.getId()
        );
    }

    private StepUpdateEntity newEntity(UserEntity user, LocalDate date) {
        StepUpdateEntity entity = new StepUpdateEntity();
        entity.setUser(user);
        entity.setDate(date);
        return entity;
    }

    private void validateItem(StepSyncItemDTO item) {
        ZoneId zone;
        try {
            zone = ZoneId.of(item.getTimezone());
        } catch (DateTimeException ex) {
            throw new StepValidationException("Neplatná časová zóna: " + item.getTimezone());
        }
        LocalDate measuredDate = item.getMeasuredUntil().atZoneSameInstant(zone).toLocalDate();
        if (!measuredDate.equals(item.getDate())) {
            throw new StepValidationException("Datum neodpovídá measuredUntil v uvedené časové zóně");
        }
        if (item.getDate().isAfter(LocalDate.now(zone))) {
            throw new StepValidationException("Nelze synchronizovat kroky z budoucnosti");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new StepValidationException("Datum od nesmí být po datu do");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw new StepValidationException("Najednou lze načíst nejvýše 366 dní");
        }
    }

    private StepDailyDTO toDTO(StepUpdateEntity entity) {
        return new StepDailyDTO(entity.getId(), entity.getDate(), entity.getStepNumber(),
                entity.getSource(), entity.getTimezone(), entity.getMeasuredUntil(), entity.getUpdateTime());
    }

    private record StepUpsertResult(StepUpdateEntity entity, boolean changed) {
    }
}
