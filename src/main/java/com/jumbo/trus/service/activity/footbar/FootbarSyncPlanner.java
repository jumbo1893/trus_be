package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.entity.footbar.FootbarSyncState;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.footbar.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FootbarSyncPlanner {
    private final FootbarAccountRepository accounts;
    private final FootbarSyncStateRepository states;
    private final FootbarSessionRepository sessions;
    private final MatchRepository matches;
    private final FootbarAutoSyncProperties properties;
    private final com.jumbo.trus.repository.football.FootballMatchRepository footballMatches;
    private final com.jumbo.trus.repository.participation.MatchParticipationRepository participation;

    public record Work(Long accountId, Long userId, Instant connectionAt, Set<Long> teamIds, boolean history) {}

    @Transactional(readOnly = true)
    public List<Work> plan(Instant now) {
        Instant latestStart = now.minus(properties.getMatchDurationMinutes(), ChronoUnit.MINUTES);
        List<MatchEntity> recent = matches.findFootbarSyncWindow(
                Date.from(latestStart.minus(properties.getWindowMinutes(), ChronoUnit.MINUTES)), Date.from(latestStart));
        var official = footballMatches.findFootbarSyncWindow(
                Date.from(latestStart.minus(properties.getWindowMinutes(), ChronoUnit.MINUTES)), Date.from(latestStart));
        List<Work> work = new ArrayList<>();
        for (FootbarAccountEntity account : accounts.findAll()) {
            if (account.getUser() == null) continue;
            FootbarSyncState state = states.findById(account.getId()).orElse(new FootbarSyncState());
            Instant linked = account.getLinkedAt() == null ? Instant.EPOCH : account.getLinkedAt();
            boolean disconnected = account.getRefreshToken() == null || account.getRefreshToken().isBlank();
            // Already notified: wait for the owner to reconnect, not another API retry.
            if (disconnected && state.isReconnectRequired() && state.getLastAttemptAt() != null
                    && !state.getLastAttemptAt().isBefore(linked)) continue;
            boolean history = state.getImportedConnectionAt() == null || state.getImportedConnectionAt().isBefore(linked);
            Set<Long> teams = new LinkedHashSet<>();
            for (UserTeamRole role : account.getUser().getTeamRoles()) {
                if (role.getAppTeam() == null) continue;
                Long teamId = role.getAppTeam().getId();
                if (history || disconnected) teams.add(teamId);
                for (MatchEntity match : recent) {
                    if (!teamId.equals(match.getAppTeam().getId())) continue;
                    Instant end = match.getDate().toInstant().plus(properties.getMatchDurationMinutes(), ChronoUnit.MINUTES);
                    boolean firstPass = state.getLastSuccessAt() == null || state.getLastSuccessAt().isBefore(end);
                    boolean participant = role.getPlayer() != null && match.getPlayerList() != null
                            && match.getPlayerList().stream().anyMatch(p -> p.getId() == role.getPlayer().getId());
                    if (!participant && role.getPlayer() != null && match.getFootballMatch() != null
                            && (match.getPlayerList() == null || match.getPlayerList().isEmpty())) {
                        participant = participation.findByFootballMatchIdAndAppTeamIdAndPlayerId(
                                match.getFootballMatch().getId(), teamId, role.getPlayer().getId())
                                .map(p -> p.getStatus() == com.jumbo.trus.entity.participation.MatchParticipationStatus.ATTENDING)
                                .orElse(false);
                    }
                    // First import checks every linked team member. Subsequent imports
                    // only wait for recorded participants whose session is still absent.
                    if (firstPass || (participant && !sessions.existsByFootbarAccount_IdAndMatch_Id(account.getId(), match.getId()))) {
                        teams.add(teamId);
                    }
                }
                // Participation exists before a local match is entered by an editor.
                // Schedule directly from the official fixture in that case.
                for (var fixture : official) {
                    var footballTeam = role.getAppTeam().getTeam();
                    if (footballTeam == null || (!footballTeam.getId().equals(fixture.getHomeTeam().getId())
                            && !footballTeam.getId().equals(fixture.getAwayTeam().getId()))) continue;
                    boolean hasLocal = recent.stream().anyMatch(m -> teamId.equals(m.getAppTeam().getId())
                            && m.getFootballMatch() != null && fixture.getId().equals(m.getFootballMatch().getId()));
                    if (hasLocal) continue;
                    Instant end = fixture.getDate().toInstant().plus(properties.getMatchDurationMinutes(), ChronoUnit.MINUTES);
                    boolean firstPass = state.getLastSuccessAt() == null || state.getLastSuccessAt().isBefore(end);
                    boolean attending = role.getPlayer() != null && participation
                            .findByFootballMatchIdAndAppTeamIdAndPlayerId(fixture.getId(), teamId, role.getPlayer().getId())
                            .map(p -> p.getStatus() == com.jumbo.trus.entity.participation.MatchParticipationStatus.ATTENDING)
                            .orElse(false);
                    if (firstPass || (attending && !sessions.existsForTimeWindow(account.getId(), fixture.getDate(), Date.from(end)))) {
                        teams.add(teamId);
                    }
                }
            }
            if (!teams.isEmpty()) work.add(new Work(account.getId(), account.getUser().getId(), linked, teams, history));
        }
        return work;
    }
}
