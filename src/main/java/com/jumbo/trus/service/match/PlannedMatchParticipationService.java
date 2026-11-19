package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.match.PlannedMatchParticipationDTO;
import com.jumbo.trus.entity.PlannedMatchEntity;
import com.jumbo.trus.entity.PlannedMatchStatus;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.participation.MatchParticipationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.repository.PlannedMatchRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlannedMatchParticipationService {
    private final PlannedMatchRepository plannedMatches;
    private final MatchParticipationRepository participations;
    private final UserTeamRoleRepository roles;

    @Transactional
    public PlannedMatchParticipationDTO get(Long id, Long userId, AppTeamEntity team) {
        plannedMatches.findByIdAndAppTeamId(id, team.getId())
                .orElseThrow(() -> new NotFoundException("Plánovaný zápas nebyl nalezen."));
        var role = roles.findByUserIdAndAppTeamId(userId, team.getId()).orElseThrow();
        Long playerId = role.getPlayer() == null ? null : role.getPlayer().getId();
        var all = participations.findAllByPlannedMatchIdAndAppTeamIdOrderByPlayerNameAsc(id, team.getId());
        var result = new PlannedMatchParticipationDTO();
        result.setAttendingPlayers(names(all, MatchParticipationStatus.ATTENDING));
        result.setMaybePlayers(names(all, MatchParticipationStatus.MAYBE));
        result.setNotAttendingPlayers(names(all, MatchParticipationStatus.NOT_ATTENDING));
        result.setAttendingPlayerCount((int) all.stream().filter(p -> p.getStatus() == MatchParticipationStatus.ATTENDING
                && !p.getPlayer().isFan() && Boolean.TRUE.equals(p.getPlaying())).count());
        result.setMaybePlayerCount((int) all.stream().filter(p -> p.getStatus() == MatchParticipationStatus.MAYBE
                && !p.getPlayer().isFan()).count());
        result.setNotAttendingPlayerCount((int) all.stream().filter(p -> p.getStatus() == MatchParticipationStatus.NOT_ATTENDING
                && !p.getPlayer().isFan()).count());
        all.stream().filter(p -> playerId != null && playerId.equals(p.getPlayer().getId())).findFirst()
                .ifPresent(p -> { result.setCurrentStatus(p.getStatus()); result.setCurrentPlaying(p.getPlaying()); });
        return result;
    }

    @Transactional
    public PlannedMatchParticipationDTO respond(Long id, Long userId, AppTeamEntity team,
                                                 MatchParticipationStatus status, Boolean playing) {
        PlannedMatchEntity planned = plannedMatches.lockForTeam(id, team.getId())
                .orElseThrow(() -> new NotFoundException("Plánovaný zápas nebyl nalezen."));
        if (planned.getFootballMatch() != null) {
            throw new IllegalArgumentException("U importovaného zápasu použij účast ve fotbalovém zápase.");
        }
        if (planned.getStatus() != PlannedMatchStatus.SCHEDULED) {
            throw new IllegalArgumentException("Účast lze změnit jen u naplánované události.");
        }
        var role = roles.findByUserIdAndAppTeamId(userId, team.getId()).orElseThrow();
        if (role.getPlayer() == null || !team.getId().equals(role.getPlayer().getAppTeam().getId())) {
            throw new IllegalArgumentException("K odpovědi musí být přiřazen hráč nebo fanoušek týmu.");
        }
        var player = role.getPlayer();
        var participation = participations.findByPlannedMatchIdAndAppTeamIdAndPlayerId(id, team.getId(), player.getId())
                .orElseGet(MatchParticipationEntity::new);
        participation.setAppTeam(team);
        participation.setPlannedMatch(planned);
        participation.setPlayer(player);
        participation.setStatus(status);
        participation.setPlaying(status == MatchParticipationStatus.NOT_ATTENDING ? false
                : playing == null ? !player.isFan() : playing);
        participation.setRespondedAt(Instant.now());
        participation.setRespondedBy(player);
        participations.save(participation);
        return get(id, userId, team);
    }

    private List<String> names(List<MatchParticipationEntity> all, MatchParticipationStatus status) {
        return all.stream().filter(p -> p.getStatus() == status)
                .map(p -> p.getPlayer().getName()).toList();
    }
}
