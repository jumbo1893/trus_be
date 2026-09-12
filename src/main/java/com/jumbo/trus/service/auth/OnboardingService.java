package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.repository.football.FootballPlayerRepository;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.helper.ValidationField;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    public enum Step { PROFILE, INTRO, NOTIFICATIONS, STEPS, FOOTBAR }
    public record State(boolean available, boolean autoShow, Set<Step> completed) {}
    public record Candidate(PlayerDTO player, boolean occupied, int score) {}
    public record FootballCandidate(Long id, String name, Long linkedPlayerId) {}
    public record Profiles(String name, PlayerDTO currentPlayer, List<Candidate> candidates, List<FootballCandidate> footballPlayers) {}
    public record PairRequest(Long playerId, String name, LocalDate birthday, boolean fan, Long footballPlayerId) {
        public PairRequest(Long playerId, String name, LocalDate birthday, boolean fan) { this(playerId, name, birthday, fan, null); }
    }

    private final UserService userService;
    private final UserRepository users;
    private final AppTeamService teams;
    private final UserTeamRoleRepository roles;
    private final PlayerService players;
    private final FootballPlayerRepository footballPlayers;

    @Transactional(readOnly = true)
    public State state() {
        return state(userService.findById(userService.getCurrentUserEntity().getId()));
    }

    static State state(UserEntity user) {
        int mask = Optional.ofNullable(user.getOnboardingCompleted()).orElse(0);
        Set<Step> completed = EnumSet.noneOf(Step.class);
        for (Step step : Step.values()) if ((mask & (1 << step.ordinal())) != 0) completed.add(step);
        boolean available = user.getOnboardingCompleted() != null;
        return new State(available, available && !Boolean.TRUE.equals(user.getOnboardingStarted()), completed);
    }

    @Transactional
    public State advance(Step completedStep) {
        UserEntity user = lockCurrentUser();
        user.setOnboardingStarted(true);
        int mask = Optional.ofNullable(user.getOnboardingCompleted()).orElse(0);
        if (completedStep != null) mask |= 1 << completedStep.ordinal();
        user.setOnboardingCompleted(mask);
        return state(user);
    }

    @Transactional(readOnly = true)
    public Profiles profiles() {
        var team = teams.getCurrentAppTeamOrThrow();
        var user = userService.findById(userService.getCurrentUserEntity().getId());
        var role = roles.findByUserIdAndAppTeamId(user.getId(), team.getId()).orElseThrow();
        Set<Long> occupied = new HashSet<>();
        for (var assignment : roles.findAllByAppTeamId(team.getId())) {
            if (!assignment.getUser().getId().equals(user.getId()) && assignment.getPlayer() != null)
                occupied.add(assignment.getPlayer().getId());
        }
        var candidates = players.getAll(team.getId()).stream()
                .map(player -> new Candidate(player, occupied.contains(player.getId()), Math.max(
                        similarity(user.getName(), player.getName()),
                        similarity(user.getName(), player.getFootballPlayer() == null ? null : player.getFootballPlayer().getName()))))
                .sorted(Comparator.comparing(Candidate::occupied)
                        .thenComparing(Comparator.comparingInt(Candidate::score).reversed())
                        .thenComparing(candidate -> candidate.player().getName()))
                .toList();
        var footballCandidates = teamFootballPlayers(team).stream()
                .map(p -> new FootballCandidate(p.getId(), p.getName(), p.getPlayer() == null ? null : p.getPlayer().getId()))
                .toList();
        return new Profiles(user.getName(), role.getPlayer() == null ? null : players.getPlayer(role.getPlayer().getId()), candidates, footballCandidates);
    }

    @Transactional
    public PlayerDTO pair(PairRequest request) {
        // Serialize own creation/retries and progress writes. Other users are
        // excluded by the existing shared player lock in pairPlayerToRole.
        var user = lockCurrentUser();
        var team = teams.getCurrentAppTeamOrThrow();
        UserTeamRole role = roles.findByUserIdAndAppTeamId(user.getId(), team.getId()).orElseThrow();
        if (role.getPlayer() != null) {
            if (request.playerId() != null && request.playerId() != role.getPlayer().getId())
                throw invalid("Profil už je propojený. Změň ho v nastavení profilu.");
            markProfile(user);
            linkFootballPlayer(role.getPlayer(), request.footballPlayerId(), team);
            return players.getPlayer(role.getPlayer().getId());
        }
        long playerId;
        if (request.playerId() != null) {
            if (request.playerId() <= 0) throw invalid("Vyber platný profil.");
            playerId = request.playerId();
        } else {
            if (request.name() == null || request.name().isBlank() || request.name().trim().length() > 100)
                throw invalid("Vyplň jméno nebo přezdívku (nejvýše 100 znaků).");
            LocalDate today = LocalDate.now(ZoneId.of("Europe/Prague"));
            if (request.birthday() == null || request.birthday().isAfter(today) || request.birthday().isBefore(today.minusYears(120)))
                throw invalid("Vyplň platné datum narození.");
            // Do not grant editing rights: this endpoint only creates one's own
            // profile and pairs it atomically; general player editing stays guarded.
            PlayerDTO draft = new PlayerDTO();
            draft.setName(request.name().trim());
            draft.setBirthday(Date.from(request.birthday().atStartOfDay(ZoneId.of("Europe/Prague")).toInstant()));
            draft.setFan(request.fan());
            draft.setActive(true);
            playerId = players.addPlayer(draft, team).getId();
        }
        PlayerEntity paired = teams.pairPlayerToRole(role, user.getId(), playerId, team);
        linkFootballPlayer(paired, request.footballPlayerId(), team);
        markProfile(user);
        return players.getPlayer(playerId);
    }

    private UserEntity lockCurrentUser() {
        return users.findByIdForUpdate(userService.getCurrentUserEntity().getId()).orElseThrow();
    }

    private List<FootballPlayerEntity> teamFootballPlayers(AppTeamEntity team) {
        if (team.getTeam() == null) return List.of();
        Map<Long, FootballPlayerEntity> result = new LinkedHashMap<>();
        footballPlayers.findAllByTeamId(team.getTeam().getId()).forEach(p -> result.put(p.getId(), p));
        footballPlayers.findAllByTeamIdWithInactive(team.getTeam().getId()).forEach(p -> result.put(p.getId(), p));
        return result.values().stream().sorted(Comparator.comparing(FootballPlayerEntity::getName)).toList();
    }

    private void linkFootballPlayer(PlayerEntity player, Long footballId, AppTeamEntity team) {
        if (footballId == null) return; // Omission preserves an existing link.
        if (teamFootballPlayers(team).stream().noneMatch(p -> p.getId().equals(footballId)))
            throw invalid("Fotbalový hráč nepatří do vybraného týmu.");
        var football = footballPlayers.findByIdForUpdate(footballId).orElseThrow(() -> invalid("Fotbalový hráč už není dostupný."));
        if (football.getPlayer() != null && football.getPlayer().getId() != player.getId())
            throw invalid("Fotbalový hráč už je propojený s jiným profilem.");
        if (player.getFootballPlayer() != null) {
            if (player.getFootballPlayer().getId().equals(footballId)) return;
            throw invalid("Profil již má jiného fotbalového hráče. Změnu proveď v nastavení profilu.");
        }
        // Use normal edit lifecycle (stats/outbox) inside this same transaction.
        // Only one's own freshly paired profile is editable through onboarding.
        var dto = players.getPlayer(player.getId());
        var footballDto = new FootballPlayerDTO(); footballDto.setId(footballId);
        dto.setFootballPlayer(footballDto);
        players.editPlayer(player.getId(), dto);
    }

    private void markProfile(UserEntity user) {
        user.setOnboardingStarted(true);
        user.setOnboardingCompleted(Optional.ofNullable(user.getOnboardingCompleted()).orElse(0) | 1);
    }

    private FieldValidationException invalid(String message) {
        return new FieldValidationException(message, List.of(new ValidationField("player", message)));
    }

    // Suggestions only, never an automatic identity claim. Accent/case/order
    // differences and small typos are tolerated; e-mail is never used to match.
    static int similarity(String first, String second) {
        String a = normalize(first), b = normalize(second);
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return 100;
        Set<String> tokens = new HashSet<>(Arrays.asList(a.split(" ")));
        if (Arrays.stream(b.split(" ")).anyMatch(t -> t.length() >= 3 && tokens.contains(t))) return 80;
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1]; current[0] = i;
            for (int j = 1; j <= b.length(); j++) current[j] = Math.min(Math.min(current[j-1] + 1, previous[j] + 1),
                    previous[j-1] + (a.charAt(i-1) == b.charAt(j-1) ? 0 : 1));
            previous = current;
        }
        return Math.max(0, 100 - 100 * previous[b.length()] / Math.max(a.length(), b.length()));
    }

    private static String normalize(String value) {
        String text = Optional.ofNullable(value).orElse("");
        return Normalizer.normalize(text.substring(0, Math.min(100, text.length())), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
