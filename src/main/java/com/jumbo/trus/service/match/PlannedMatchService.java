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
import com.jumbo.trus.service.SeasonService;
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

    @Transactional
    public List<PlannedMatchDTO> list(AppTeamEntity team) {
        return plannedMatches.findUpcomingForTeam(team.getId(), PlannedMatchStatus.SCHEDULED, new Date())
                .stream().map(this::toDto).toList();
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
        PlannedMatchEntity planned = lock(id, team);
        completeIfDue(planned, team);
        return toDto(planned);
    }

    @Transactional
    public PlannedMatchDTO getByFootballMatch(Long footballMatchId, AppTeamEntity team) {
        PlannedMatchEntity planned = plannedMatches
                .findByAppTeamIdAndFootballMatchId(team.getId(), footballMatchId)
                .orElseGet(() -> {
                    var football = footballMatches.findById(footballMatchId)
                            .orElseThrow(() -> new NotFoundException("Fotbalový zápas nebyl nalezen."));
                    Long teamId = team.getTeam() == null ? null : team.getTeam().getId();
                    if (teamId == null || football.getDate() == null
                            || football.getHomeTeam() == null || football.getAwayTeam() == null
                            || (!teamId.equals(football.getHomeTeam().getId())
                            && !teamId.equals(football.getAwayTeam().getId()))) {
                        throw new NotFoundException("Fotbalový zápas nebyl nalezen v týmu.");
                    }
                    syncFootballMatch(football, team);
                    return plannedMatches.findByAppTeamIdAndFootballMatchId(team.getId(), footballMatchId)
                            .orElseThrow();
                });
        return get(planned.getId(), team);
    }

    @Transactional
    public PlannedMatchDTO complete(Long id, AppTeamEntity team) {
        PlannedMatchEntity planned = lock(id, team);
        if (planned.getStatus() == PlannedMatchStatus.CANCELLED) {
            throw new IllegalStateException("Zrušenou událost nelze dokončit.");
        }
        finish(planned, team);
        return toDto(planned);
    }

    @Transactional
    public PlannedMatchDTO update(Long id, PlannedMatchDTO input, AppTeamEntity team) {
        PlannedMatchEntity planned = lock(id, team);
        if (planned.getStatus() == PlannedMatchStatus.COMPLETED) {
            throw new IllegalStateException("Dokončený zápas upravte v detailu zápasu.");
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

    private void completeIfDue(PlannedMatchEntity planned, AppTeamEntity team) {
        if (planned.getStatus() == PlannedMatchStatus.SCHEDULED && planned.getDate().before(new Date())) finish(planned, team);
    }

    private void finish(PlannedMatchEntity planned, AppTeamEntity team) {
        if (planned.getStatus() == PlannedMatchStatus.COMPLETED) return;
        if (planned.getFootballMatch() != null) {
            var existing = matches.findAllByFootballMatchId(planned.getFootballMatch().getId(), team.getId());
            if (existing.isPresent()) {
                planned.setMatch(existing.get());
                planned.setStatus(PlannedMatchStatus.COMPLETED);
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
        match.setPlayerIdList(List.of());
        if (planned.getFootballMatch() != null) {
            var football = new com.jumbo.trus.dto.football.FootballMatchDTO();
            football.setId(planned.getFootballMatch().getId());
            match.setFootballMatch(football);
        }
        var saved = matchCommands.addMatch(match, team);
        planned.setMatch(matches.findById(saved.getId()).orElseThrow());
        planned.setStatus(PlannedMatchStatus.COMPLETED);
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
        dto.setStatus(planned.getStatus());
        dto.setMatchId(planned.getMatch() == null ? null : planned.getMatch().getId());
        dto.setDescription(planned.getDescription());
        dto.setHasPhoto(planned.isHasPhoto());
        return dto;
    }
}
