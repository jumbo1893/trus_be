package com.jumbo.trus.service.season;

import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.service.achievement.SeasonAchievementTiming;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.jumbo.trus.config.Config.OTHER_SEASON_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomaticSeasonService {
    private final AppTeamRepository appTeamRepository;
    private final FootballMatchRepository footballMatchRepository;
    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;
    private final OutboxEventService outboxEventService;
    private Clock clock = Clock.system(SeasonAchievementTiming.ZONE);

    @Transactional
    public void synchronizeAfterMatchImport() {
        LocalDate today = LocalDate.now(clock.withZone(SeasonAchievementTiming.ZONE));
        for (AppTeamEntity appTeam : appTeamRepository.findAll()) {
            synchronizeTeam(appTeam, today);
        }
    }

    private void synchronizeTeam(AppTeamEntity appTeam, LocalDate today) {
        Map<Period, Bounds> imported = new LinkedHashMap<>();
        if (appTeam.getTeam() != null && appTeam.getTeam().getId() != null) {
            for (Date value : footballMatchRepository.findAllMatchDatesByTeamId(appTeam.getTeam().getId())) {
                LocalDateTime local = value.toInstant().atZone(SeasonAchievementTiming.ZONE).toLocalDateTime();
                Bounds matchDay = new Bounds(startOfDay(local.toLocalDate()), endOfDay(local.toLocalDate()));
                imported.compute(new Period(local.getYear(), Half.fromMonth(local.getMonthValue())),
                        (ignored, bounds) -> bounds == null
                                ? matchDay : bounds.include(matchDay));
            }
        }
        imported.forEach((period, bounds) -> synchronizePeriod(appTeam, period, bounds));

        Period spring = new Period(today.getYear(), Half.SPRING);
        if (!today.isBefore(LocalDate.of(today.getYear(), 3, 1)) && !imported.containsKey(spring)) {
            synchronizePeriod(appTeam, spring, defaultBounds(spring));
        }
        Period autumn = new Period(today.getYear(), Half.AUTUMN);
        if (!today.isBefore(LocalDate.of(today.getYear(), 9, 1)) && !imported.containsKey(autumn)) {
            synchronizePeriod(appTeam, autumn, defaultBounds(autumn));
        }
    }

    private void synchronizePeriod(AppTeamEntity appTeam, Period period, Bounds bounds) {
        String key = period.half().name() + "_" + period.year();
        String name = period.half().displayName + " " + period.year();
        SeasonEntity season = seasonRepository.findByAppTeamIdAndAutomaticKey(appTeam.getId(), key)
                .orElseGet(() -> seasonRepository.findFirstByAppTeamIdAndName(appTeam.getId(), name)
                        .map(existing -> {
                            existing.setAutomaticKey(key);
                            existing.setDatesManuallyEdited(true);
                            return existing;
                        })
                        .orElseGet(() -> newAutomaticSeason(appTeam, key, name, bounds)));

        boolean created = season.getId() == null;
        boolean datesChanged = !sameDate(season.getFromDate(), bounds.from())
                || !sameDate(season.getToDate(), bounds.to());
        season.setName(name);
        if (!season.isDatesManuallyEdited() && datesChanged) {
            season.setFromDate(bounds.from());
            season.setToDate(bounds.to());
            season.setAchievementEventForEnd(null);
        }
        season = seasonRepository.saveAndFlush(season);

        matchRepository.assignUnclassifiedMatchesToSeason(
                appTeam.getId(), season.getId(), OTHER_SEASON_ID,
                season.getFromDate(), season.getToDate());
        Set<Long> affectedMatches = matchRepository.findMatchIdsBySeason(season.getId());
        if (created || (!season.isDatesManuallyEdited() && datesChanged)) {
            outboxEventService.createEventForTeam(
                    created ? OutboxEventType.SEASON_CREATED : OutboxEventType.SEASON_UPDATED,
                    OutboxAggregateType.SEASON,
                    season.getId(),
                    created ? OutboxEventPayloadFactory.seasonCreated(affectedMatches)
                            : OutboxEventPayloadFactory.seasonUpdated(affectedMatches),
                    appTeam.getId(), -1L);
            log.info("Automatically {} season {} for appTeamId={}, from={}, to={}",
                    created ? "created" : "updated", name, appTeam.getId(), season.getFromDate(), season.getToDate());
        }
    }

    private SeasonEntity newAutomaticSeason(AppTeamEntity appTeam, String key, String name, Bounds bounds) {
        SeasonEntity season = new SeasonEntity();
        season.setAppTeam(appTeam);
        season.setAutomaticKey(key);
        season.setName(name);
        season.setFromDate(bounds.from());
        season.setToDate(bounds.to());
        return season;
    }

    private Bounds defaultBounds(Period period) {
        return period.half() == Half.SPRING
                ? new Bounds(startOfDay(period.year(), 3, 1), endOfDay(period.year(), 6, 30))
                : new Bounds(startOfDay(period.year(), 9, 1), endOfDay(period.year(), 12, 31));
    }

    private Date startOfDay(int year, int month, int day) {
        return startOfDay(LocalDate.of(year, month, day));
    }

    private Date endOfDay(int year, int month, int day) {
        return endOfDay(LocalDate.of(year, month, day));
    }

    private Date startOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(SeasonAchievementTiming.ZONE).toInstant());
    }

    private Date endOfDay(LocalDate date) {
        return Date.from(date.atTime(LocalTime.MAX)
                .atZone(SeasonAchievementTiming.ZONE).toInstant());
    }

    private boolean sameDate(Date first, Date second) {
        return first != null && second != null && first.getTime() == second.getTime();
    }

    private enum Half {
        SPRING("Jaro"), AUTUMN("Podzim");
        private final String displayName;
        Half(String displayName) { this.displayName = displayName; }
        static Half fromMonth(int month) { return month <= 6 ? SPRING : AUTUMN; }
    }

    private record Period(int year, Half half) { }
    private record Bounds(Date from, Date to) {
        private Bounds include(Bounds value) {
            return new Bounds(value.from.before(from) ? value.from : from,
                    value.to.after(to) ? value.to : to);
        }
    }
}
