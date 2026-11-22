package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.PlannedMatchDTO;
import com.jumbo.trus.entity.PlannedMatchEntity;
import com.jumbo.trus.entity.PlannedMatchPhotoEntity;
import com.jumbo.trus.entity.PlannedMatchStatus;
import com.jumbo.trus.entity.PlannedMatchType;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlannedMatchRepository;
import com.jumbo.trus.repository.PlannedMatchPhotoRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import com.jumbo.trus.entity.participation.MatchParticipationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.appnotice.AppVersionComparator;
import com.jumbo.trus.service.header.HeaderManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.Date;
import java.util.List;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlannedMatchService {
    private final PlannedMatchRepository plannedMatches;
    private final PlannedMatchPhotoRepository photos;
    private final MatchRepository matches;
    private final SeasonRepository seasons;
    private final FootballMatchRepository footballMatches;
    private final SeasonService seasonService;
    private final MatchCommandService matchCommands;
    private final MatchParticipationRepository participations;
    private final HeaderManager headers;

    /** Planned matches older than this are never auto-started by listing, so stale data stays untouched. */
    private static final long ACTIVE_WINDOW_MS = 24L * 60 * 60 * 1000;

    @Transactional
    public List<PlannedMatchDTO> list(AppTeamEntity team) {
        return upcoming(team).stream().map(this::toDto).toList();
    }

    /** Scheduled and in-progress events (in-progress first), with due status transitions applied. */
    @Transactional
    public List<PlannedMatchEntity> upcoming(AppTeamEntity team) {
        Date now = new Date();
        // Started rows are loaded through the pessimistic lock so that concurrent
        // requests cannot create the linked match twice.
        var inProgress = plannedMatches.findStartedIds(team.getId(),
                        List.of(PlannedMatchStatus.SCHEDULED, PlannedMatchStatus.IN_PROGRESS),
                        new Date(now.getTime() - ACTIVE_WINDOW_MS), now).stream()
                .map(id -> refreshed(id, team))
                .filter(planned -> planned.getStatus() == PlannedMatchStatus.IN_PROGRESS);
        var scheduled = plannedMatches.findUpcomingForTeam(team.getId(), PlannedMatchStatus.SCHEDULED, now).stream();
        return java.util.stream.Stream.concat(inProgress, scheduled).toList();
    }

    private PlannedMatchEntity refreshed(Long id, AppTeamEntity team) {
        PlannedMatchEntity planned = lock(id, team);
        refreshStatus(planned, team);
        return planned;
    }

    @Transactional
    public PlannedMatchDTO create(PlannedMatchDTO input, AppTeamEntity team) {
        PlannedMatchEntity planned = new PlannedMatchEntity();
        planned.setAppTeam(team);
        apply(planned, input, team);
        return toDto(plannedMatches.save(planned));
    }

    @Transactional
    public PlannedMatchDTO get(Long id, AppTeamEntity team) {
        return toDto(refreshed(id, team));
    }

    @Transactional
    public PlannedMatchDTO getByFootballMatch(Long footballMatchId, AppTeamEntity team) {
        PlannedMatchEntity planned = plannedMatches
                .findByAppTeamIdAndFootballMatchId(team.getId(), footballMatchId)
                .orElseGet(() -> {
                    var football = teamFixture(footballMatchId, team);
                    syncFootballMatch(football, team);
                    return plannedMatches.findByAppTeamIdAndFootballMatchId(team.getId(), footballMatchId)
                            .orElseThrow();
                });
        return get(planned.getId(), team);
    }

    /** Events that were already played, started or cancelled (newest first). */
    @Transactional
    public List<PlannedMatchDTO> resettable(AppTeamEntity team) {
        return plannedMatches.findAllByAppTeamIdOrderByDateDesc(team.getId()).stream()
                .filter(planned -> planned.getStatus() != PlannedMatchStatus.SCHEDULED)
                .limit(30)
                .map(this::toDto)
                .toList();
    }

    /**
     * Deletes the recorded match of an event (if any) and schedules the event
     * again; also restores events whose match was deleted.
     */
    @Transactional
    public PlannedMatchDTO reset(Long id, AppTeamEntity team) {
        PlannedMatchEntity planned = lock(id, team);
        Long matchId = planned.getMatch() != null ? planned.getMatch().getId()
                : planned.getFootballMatch() == null ? null
                : matches.findAllByFootballMatchId(planned.getFootballMatch().getId(), team.getId())
                        .map(com.jumbo.trus.entity.MatchEntity::getId).orElse(null);
        if (matchId != null) {
            matchCommands.deleteMatch(matchId, team);
        }
        planned.setStatus(PlannedMatchStatus.SCHEDULED);
        return toDto(planned);
    }

    private FootballMatchEntity teamFixture(Long footballMatchId, AppTeamEntity team) {
        var football = footballMatches.findById(footballMatchId)
                .orElseThrow(() -> new NotFoundException("Fotbalový zápas nebyl nalezen."));
        Long teamId = team.getTeam() == null ? null : team.getTeam().getId();
        if (teamId == null || football.getDate() == null
                || football.getHomeTeam() == null || football.getAwayTeam() == null
                || (!teamId.equals(football.getHomeTeam().getId())
                && !teamId.equals(football.getAwayTeam().getId()))) {
            throw new NotFoundException("Fotbalový zápas nebyl nalezen v týmu.");
        }
        return football;
    }

    @Transactional
    public PlannedMatchDTO complete(Long id, AppTeamEntity team) {
        return changeStatus(id, PlannedMatchStatus.COMPLETED, team);
    }

    @Transactional
    public PlannedMatchDTO changeStatus(Long id, PlannedMatchStatus target, AppTeamEntity team) {
        PlannedMatchEntity planned = refreshed(id, team);
        PlannedMatchStatus current = planned.getStatus();
        if (current == target) return toDto(planned);
        switch (target) {
            case SCHEDULED -> {
                if (current != PlannedMatchStatus.CANCELLED) {
                    throw new IllegalStateException("Obnovit lze jen zrušenou událost.");
                }
                planned.setStatus(PlannedMatchStatus.SCHEDULED);
                refreshStatus(planned, team);
            }
            case IN_PROGRESS -> {
                if (current != PlannedMatchStatus.SCHEDULED) {
                    throw new IllegalStateException("Zahájit lze jen naplánovanou událost.");
                }
                planned.setDate(new Date());
                start(planned, team);
            }
            case COMPLETED -> {
                if (current == PlannedMatchStatus.CANCELLED) {
                    throw new IllegalStateException("Zrušenou událost nelze dokončit.");
                }
                ensureMatch(planned, team);
                planned.setStatus(PlannedMatchStatus.COMPLETED);
            }
            case CANCELLED -> {
                if (current != PlannedMatchStatus.SCHEDULED) {
                    throw new IllegalStateException("Zrušit lze jen naplánovanou událost.");
                }
                planned.setStatus(PlannedMatchStatus.CANCELLED);
            }
        }
        return toDto(planned);
    }

    @Transactional
    public PlannedMatchDTO update(Long id, PlannedMatchDTO input, AppTeamEntity team) {
        PlannedMatchEntity planned = lock(id, team);
        if (planned.getStatus() == PlannedMatchStatus.COMPLETED || planned.getStatus() == PlannedMatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Probíhající nebo dokončený zápas upravte v detailu zápasu.");
        }
        apply(planned, input, team);
        planned.setStatus(input.getStatus() == PlannedMatchStatus.CANCELLED
                ? PlannedMatchStatus.CANCELLED : PlannedMatchStatus.SCHEDULED);
        return toDto(planned);
    }

    @Transactional
    public void savePhoto(Long id, AppTeamEntity team, MultipartFile image) throws IOException {
        if (image.isEmpty() || image.getSize() > 2_000_000) {
            throw new IllegalArgumentException("Fotka musí mít nejvýše 2 MB.");
        }
        byte[] bytes = image.getBytes();
        var readers = ImageIO.getImageReaders(ImageIO.createImageInputStream(new ByteArrayInputStream(bytes)));
        if (!readers.hasNext() || !"JPEG".equalsIgnoreCase(readers.next().getFormatName())) {
            throw new IllegalArgumentException("Soubor není platný obrázek.");
        }
        lock(id, team).setHasPhoto(true);
        PlannedMatchPhotoEntity photo = photos.findById(id).orElseGet(PlannedMatchPhotoEntity::new);
        photo.setPlannedMatchId(id);
        photo.setImage(bytes);
        photos.save(photo);
    }

    @Transactional
    public byte[] photo(Long id, AppTeamEntity team) {
        lock(id, team);
        return photos.findById(id).map(PlannedMatchPhotoEntity::getImage)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void deletePhoto(Long id, AppTeamEntity team) {
        lock(id, team).setHasPhoto(false);
        photos.deleteById(id);
    }

    @Transactional
    public void syncFootballMatch(FootballMatchEntity footballMatch, AppTeamEntity team) {
        PlannedMatchEntity planned = plannedMatches.findByAppTeamIdAndFootballMatchId(team.getId(), footballMatch.getId())
                .orElseGet(() -> {
                    PlannedMatchEntity created = new PlannedMatchEntity();
                    created.setAppTeam(team);
                    created.setFootballMatch(footballMatch);
                    created.setType(PlannedMatchType.LEAGUE);
                    return created;
                });
        if (planned.getStatus() == PlannedMatchStatus.COMPLETED || planned.getStatus() == PlannedMatchStatus.CANCELLED) return;
        planned.setDate(footballMatch.getDate());
        boolean home = team.getTeam() != null && footballMatch.getHomeTeam() != null
                && team.getTeam().getId().equals(footballMatch.getHomeTeam().getId());
        planned.setHome(home);
        planned.setOpponent(home ? footballMatch.getAwayTeam().getName() : footballMatch.getHomeTeam().getName());
        plannedMatches.save(planned);
    }

    private void apply(PlannedMatchEntity planned, PlannedMatchDTO input, AppTeamEntity team) {
        if (input.getType() != PlannedMatchType.TRAINING && input.getType() != PlannedMatchType.TEAMBUILDING
                && (input.getOpponent() == null || input.getOpponent().isBlank())) {
            throw new IllegalArgumentException("U zápasu je povinný soupeř.");
        }
        planned.setDate(input.getDate());
        planned.setType(input.getType());
        planned.setOpponent(input.getOpponent());
        planned.setHome(input.getType() == PlannedMatchType.TRAINING || input.getType() == PlannedMatchType.TEAMBUILDING
                ? null : input.getHome());
        planned.setDescription(input.getDescription());
        planned.setSeason(input.getSeasonId() == null ? null : seasons.findByIdAndAppTeamId(input.getSeasonId(), team.getId())
                .orElseThrow(() -> new NotFoundException("Sezona nebyla nalezena v týmu.")));
        // The imported football match is linked only by the football synchronization job.
    }

    private void refreshStatus(PlannedMatchEntity planned, AppTeamEntity team) {
        Date now = new Date();
        if (planned.getStatus() == PlannedMatchStatus.SCHEDULED && !planned.getDate().after(now)) {
            start(planned, team);
        }
        if (planned.getStatus() == PlannedMatchStatus.IN_PROGRESS && !endOf(planned, team).after(now)) {
            planned.setStatus(PlannedMatchStatus.COMPLETED);
        }
    }

    private void start(PlannedMatchEntity planned, AppTeamEntity team) {
        ensureMatch(planned, team);
        planned.setStatus(PlannedMatchStatus.IN_PROGRESS);
    }

    private static Date endOf(PlannedMatchEntity planned, AppTeamEntity team) {
        long minutes = 2L * team.getMatchHalfMinutes() + team.getMatchBreakMinutes();
        return new Date(planned.getDate().getTime() + minutes * 60_000);
    }

    private void ensureMatch(PlannedMatchEntity planned, AppTeamEntity team) {
        if (planned.getMatch() != null) return;
        if (planned.getFootballMatch() != null) {
            var existing = matches.findAllByFootballMatchId(planned.getFootballMatch().getId(), team.getId());
            if (existing.isPresent()) {
                planned.setMatch(existing.get());
                return;
            }
        }
        MatchDTO match = new MatchDTO();
        match.setName(planned.getOpponent() == null ? planned.getType().name() : planned.getOpponent());
        match.setType(planned.getType());
        match.setDescription(planned.getDescription());
        match.setDate(planned.getDate());
        match.setHome(Boolean.TRUE.equals(planned.getHome()));
        match.setSeasonId(planned.getSeason() == null ? com.jumbo.trus.config.Config.AUTOMATIC_SEASON_ID
                : planned.getSeason().getId());
        match.setPlayerIdList(attendingPlayerIds(planned, team));
        if (planned.getFootballMatch() != null) {
            var football = new com.jumbo.trus.dto.football.FootballMatchDTO();
            football.setId(planned.getFootballMatch().getId());
            match.setFootballMatch(football);
        }
        var saved = matchCommands.addMatch(match, team);
        planned.setMatch(matches.findById(saved.getId()).orElseThrow());
    }

    /** Lineup pre-filled from attendance: attending players and fans alike are match participants. */
    private List<Long> attendingPlayerIds(PlannedMatchEntity planned, AppTeamEntity team) {
        List<MatchParticipationEntity> attending = planned.getFootballMatch() != null
                ? participations.findAllByFootballMatchIdAndAppTeamIdAndStatusOrderByPlayerNameAsc(
                        planned.getFootballMatch().getId(), team.getId(), MatchParticipationStatus.ATTENDING)
                : participations.findAllByPlannedMatchIdAndAppTeamIdOrderByPlayerNameAsc(planned.getId(), team.getId())
                        .stream().filter(p -> p.getStatus() == MatchParticipationStatus.ATTENDING).toList();
        return attending.stream().map(p -> p.getPlayer().getId()).distinct().toList();
    }

    private PlannedMatchEntity lock(Long id, AppTeamEntity team) {
        return plannedMatches.lockForTeam(id, team.getId())
                .orElseThrow(() -> new NotFoundException("Plánovaný zápas nebyl nalezen v týmu."));
    }

    private PlannedMatchDTO toDto(PlannedMatchEntity planned) {
        PlannedMatchDTO dto = new PlannedMatchDTO();
        dto.setId(planned.getId());
        dto.setDate(planned.getDate());
        dto.setOpponent(planned.getOpponent());
        dto.setType(planned.getType());
        dto.setHome(planned.getHome());
        dto.setSeasonId(planned.getSeason() == null ? null : planned.getSeason().getId());
        dto.setFootballMatchId(planned.getFootballMatch() == null ? null : planned.getFootballMatch().getId());
        if (planned.getFootballMatch() != null) {
            var football = planned.getFootballMatch();
            var opponent = Boolean.TRUE.equals(planned.getHome())
                    ? football.getAwayTeam() : football.getHomeTeam();
            dto.setOpponentLogoUrl(opponent == null ? null : opponent.getLogoUrl());
            dto.setFootballRound(football.getRound());
            dto.setFootballStadium(football.getStadium());
        }
        boolean legacyClient = AppVersionComparator.isWithinRange(headers.getAppVersionHeader(), "0", "12.0.999");
        dto.setStatus(legacyClient && planned.getStatus() == PlannedMatchStatus.IN_PROGRESS
                ? PlannedMatchStatus.COMPLETED : planned.getStatus());
        dto.setHalfMinutes(planned.getAppTeam().getMatchHalfMinutes());
        dto.setBreakMinutes(planned.getAppTeam().getMatchBreakMinutes());
        dto.setMatchId(planned.getMatch() == null ? null : planned.getMatch().getId());
        dto.setDescription(planned.getDescription());
        dto.setHasPhoto(planned.isHasPhoto());
        return dto;
    }
}
