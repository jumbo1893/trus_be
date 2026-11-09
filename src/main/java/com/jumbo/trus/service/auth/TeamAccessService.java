package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.AppTeamJoinRequest;
import com.jumbo.trus.dto.auth.AppTeamJoinResult;
import com.jumbo.trus.dto.auth.TeamAdministrationDTO;
import com.jumbo.trus.dto.auth.TeamMemberDTO;
import com.jumbo.trus.dto.auth.UpdateJoinCodeRequest;
import com.jumbo.trus.dto.auth.UpdateTeamMemberRoleRequest;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.TeamJoinCodeEntity;
import com.jumbo.trus.entity.auth.TeamRole;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.auth.TeamJoinCodeRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.AppOpenRepository;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.header.HeaderManager;
import com.jumbo.trus.service.helper.ValidationField;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TeamAccessService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GENERATED_CODE_LENGTH = 10;
    private static final Pattern CUSTOM_CODE_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9_-]{3,31}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TeamJoinCodeRepository joinCodeRepository;
    private final UserTeamRoleRepository userTeamRoleRepository;
    private final AppTeamRepository appTeamRepository;
    private final UserService userService;
    private final HeaderManager headerManager;
    private final OutboxEventService outboxEventService;
    private final AppOpenRepository appOpenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public void createJoinCodes(AppTeamEntity appTeam) {
        ensureJoinCode(appTeam, TeamRole.READER);
        ensureJoinCode(appTeam, TeamRole.EDITOR);
    }

    @Transactional
    public AppTeamJoinResult joinCurrentUser(AppTeamJoinRequest request) {
        String normalizedCode = normalizeCode(request == null ? null : request.getCode(), "joinCode");
        TeamJoinCodeEntity joinCode = joinCodeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> validationError("joinCode", "Tento kód není platný."));

        UserEntity user = userService.getCurrentUserEntity();
        AppTeamEntity appTeam = joinCode.getAppTeam();
        UserTeamRole userTeamRole = userTeamRoleRepository
                .findByUserIdAndAppTeamId(user.getId(), appTeam.getId())
                .orElseGet(() -> createUserTeamRole(user, appTeam));

        TeamRole existingRole = TeamRole.from(userTeamRole.getRole());
        if (userTeamRole.isRoleApproved() && joinCode.getGrantedRole().hasAtLeast(existingRole)
                && joinCode.getGrantedRole() != existingRole) {
            userTeamRole.setRole(joinCode.getGrantedRole().name());
            userTeamRoleRepository.save(userTeamRole);
            outboxEventService.createAuditEvent(OutboxEventType.TEAM_MEMBER_ROLE_CHANGED,
                    userTeamRole.getId(), appTeam.getId(), user.getId(),
                    java.util.Map.of("role", userTeamRole.getRole()));
        }

        userService.refreshUserInSecurityContext();
        return new AppTeamJoinResult(
                userService.getCurrentUser(),
                appTeam.getId(),
                userTeamRole.getRole()
        );
    }

    @Transactional
    public TeamAdministrationDTO getAdministration() {
        AppTeamEntity appTeam = getCurrentAppTeam();
        createJoinCodes(appTeam);
        return toAdministrationDTO(appTeam);
    }

    @Transactional
    public TeamAdministrationDTO updateJoinCode(String roleValue, UpdateJoinCodeRequest request) {
        AppTeamEntity appTeam = getCurrentAppTeam();
        TeamRole role = parseJoinCodeRole(roleValue);
        String normalizedCode = normalizeCode(request == null ? null : request.getCode(), "code");

        TeamJoinCodeEntity currentCode = joinCodeRepository
                .findByAppTeamIdAndGrantedRole(appTeam.getId(), role)
                .orElseGet(() -> newJoinCode(appTeam, role));
        Optional<TeamJoinCodeEntity> conflictingCode = joinCodeRepository.findByCode(normalizedCode);
        if (conflictingCode.isPresent()
                && !conflictingCode.get().getId().equals(currentCode.getId())) {
            throw validationError("code", "Tento kód už používá jiný tým.");
        }

        currentCode.setCode(normalizedCode);
        joinCodeRepository.save(currentCode);
        outboxEventService.createAuditEvent(OutboxEventType.TEAM_JOIN_CODE_CHANGED,
                currentCode.getId(), appTeam.getId(), userService.getCurrentUserEntity().getId());
        createJoinCodes(appTeam);
        return toAdministrationDTO(appTeam);
    }

    @Transactional
    public TeamAdministrationDTO updateMemberRole(
            Long userTeamRoleId,
            UpdateTeamMemberRoleRequest request
    ) {
        AppTeamEntity appTeam = getCurrentAppTeam();
        UserTeamRole targetRole = getRoleFromCurrentTeam(userTeamRoleId, appTeam);
        TeamRole newRole = parseMemberRole(request == null ? null : request.getRole());
        UserEntity owner = appTeam.getOwner();
        if (owner != null
                && owner.getId().equals(targetRole.getUser().getId())
                && newRole != TeamRole.ADMIN) {
            throw validationError("administrator", "Zakladateli týmu nelze odebrat administrátorská práva.");
        }

        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser.getId().equals(targetRole.getUser().getId())
                && newRole != TeamRole.ADMIN) {
            throw validationError("administrator", "Svá vlastní administrátorská práva nemůžeš odebrat.");
        }

        if (!newRole.name().equalsIgnoreCase(targetRole.getRole()) || !targetRole.isRoleApproved()) {
            targetRole.setRole(newRole.name());
            targetRole.setRoleApproved(true);
            userTeamRoleRepository.save(targetRole);
            outboxEventService.createAuditEvent(OutboxEventType.TEAM_MEMBER_ROLE_CHANGED,
                    targetRole.getId(), appTeam.getId(), currentUser.getId(),
                    java.util.Map.of("role", targetRole.getRole(),
                            "userId", targetRole.getUser().getId().toString()));
        }
        return toAdministrationDTO(appTeam);
    }

    @Transactional
    public TeamAdministrationDTO updateMemberVisibility(Long userTeamRoleId, boolean hidden) {
        AppTeamEntity appTeam = getCurrentAppTeam();
        UserEntity actor = userService.getCurrentUserEntity();
        if (appTeam.getOwner() == null || !appTeam.getOwner().getId().equals(actor.getId())) {
            throw new AuthException("Uživatele může skrývat jen zakladatel týmu.",
                    AuthException.INSUFFICIENT_RIGHTS);
        }
        UserTeamRole targetRole = getRoleFromCurrentTeam(userTeamRoleId, appTeam);
        UserEntity target = targetRole.getUser();
        if (target.getId().equals(actor.getId())) {
            throw validationError("hidden", "Zakladatel nemůže skrýt sám sebe.");
        }
        if (target.isHidden() != hidden) {
            target.setHidden(hidden);
            userRepository.save(target);
            outboxEventService.createAuditEvent(OutboxEventType.USER_UPDATED,
                    target.getId(), appTeam.getId(), actor.getId());
        }
        return toAdministrationDTO(appTeam);
    }

    private AppTeamEntity getCurrentAppTeam() {
        Long appTeamId = headerManager.getAppTeamIdHeader();
        if (appTeamId == null) {
            throw new AuthException(
                    "Pro tuto operaci je třeba uvést ID týmu v hlavičce!",
                    AuthException.MISSING_TEAM_ID
            );
        }
        return appTeamRepository.findById(appTeamId)
                .orElseThrow(() -> new NotFoundException("Tým s id " + appTeamId + " nenalezen."));
    }

    private TeamAdministrationDTO toAdministrationDTO(AppTeamEntity appTeam) {
        TeamJoinCodeEntity readerCode = ensureJoinCode(appTeam, TeamRole.READER);
        TeamJoinCodeEntity editorCode = ensureJoinCode(appTeam, TeamRole.EDITOR);
        Long ownerId = appTeam.getOwner() == null ? null : appTeam.getOwner().getId();
        String ownerName = appTeam.getOwner() == null
                ? null
                : displayName(appTeam.getOwner());

        boolean viewerIsOwner = ownerId != null && ownerId.equals(userService.getCurrentUserEntity().getId());
        List<UserTeamRole> roles = userTeamRoleRepository.findAllByAppTeamId(appTeam.getId())
                .stream().filter(role -> viewerIsOwner || !role.getUser().isHidden()).toList();
        Map<Long, java.time.Instant> lastLogins = roles.isEmpty()
                ? Map.of()
                : appOpenRepository.findLastOpenByUserIds(
                        roles.stream().map(role -> role.getUser().getId()).distinct().toList())
                        .stream().collect(Collectors.toMap(
                                AppOpenRepository.LastOpen::getUserId,
                                AppOpenRepository.LastOpen::getLastOpenedAt));
        List<TeamMemberDTO> members = roles.stream()
                .map(role -> new TeamMemberDTO(
                        role.getId(),
                        role.getUser().getId(),
                        displayName(role.getUser()),
                        role.getUser().getMail(),
                        role.getRole(),
                        ownerId != null && ownerId.equals(role.getUser().getId()),
                        lastLogins.get(role.getUser().getId()),
                        role.getUser().isHidden(),
                        role.isRoleApproved()
                ))
                .sorted(Comparator
                        .comparing(TeamMemberDTO::isOwner).reversed()
                        .thenComparing(TeamMemberDTO::getUserName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new TeamAdministrationDTO(
                appTeam.getId(),
                appTeam.getName(),
                ownerId,
                ownerName,
                userService.getCurrentUserEntity().getId(),
                readerCode.getCode(),
                editorCode.getCode(),
                members
        );
    }

    private UserTeamRole getRoleFromCurrentTeam(Long userTeamRoleId, AppTeamEntity appTeam) {
        UserTeamRole role = userTeamRoleRepository.findById(userTeamRoleId)
                .orElseThrow(() -> new NotFoundException("Člen týmu nebyl nalezen."));
        if (!appTeam.getId().equals(role.getAppTeam().getId())) {
            throw new AuthException(
                    "Člen nepatří do aktuálního týmu.",
                    AuthException.INSUFFICIENT_RIGHTS
            );
        }
        return role;
    }

    private UserTeamRole createUserTeamRole(UserEntity user, AppTeamEntity appTeam) {
        UserTeamRole userTeamRole = new UserTeamRole();
        userTeamRole.setUser(user);
        userTeamRole.setAppTeam(appTeam);
        userTeamRole.setRole(TeamRole.VIEWER.name());
        userTeamRole.setRoleApproved(false);
        UserTeamRole savedRole = userTeamRoleRepository.save(userTeamRole);
        outboxEventService.createAuditEvent(OutboxEventType.TEAM_JOINED,
                appTeam.getId(), appTeam.getId(), user.getId());
        user.getTeamRoles().add(savedRole);
        appTeam.getTeamRoles().add(savedRole);
        events.publishEvent(new TeamMemberPendingEvent(appTeam.getId(), savedRole.getId(),
                user.getId(), displayName(user)));
        return savedRole;
    }

    private TeamJoinCodeEntity ensureJoinCode(AppTeamEntity appTeam, TeamRole role) {
        return joinCodeRepository.findByAppTeamIdAndGrantedRole(appTeam.getId(), role)
                .orElseGet(() -> joinCodeRepository.save(newGeneratedJoinCode(appTeam, role)));
    }

    private TeamJoinCodeEntity newGeneratedJoinCode(AppTeamEntity appTeam, TeamRole role) {
        TeamJoinCodeEntity joinCode = newJoinCode(appTeam, role);
        joinCode.setCode(generateUniqueCode());
        return joinCode;
    }

    private TeamJoinCodeEntity newJoinCode(AppTeamEntity appTeam, TeamRole role) {
        TeamJoinCodeEntity joinCode = new TeamJoinCodeEntity();
        joinCode.setAppTeam(appTeam);
        joinCode.setGrantedRole(role);
        return joinCode;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder code = new StringBuilder(GENERATED_CODE_LENGTH);
            for (int index = 0; index < GENERATED_CODE_LENGTH; index++) {
                code.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String value = code.toString();
            if (!joinCodeRepository.existsByCode(value)) {
                return value;
            }
        }
        throw new IllegalStateException("Nepodařilo se vytvořit unikátní kód týmu.");
    }

    private TeamRole parseJoinCodeRole(String roleValue) {
        try {
            TeamRole role = TeamRole.from(roleValue);
            if (role == TeamRole.READER || role == TeamRole.EDITOR) {
                return role;
            }
        } catch (RuntimeException ignored) {
            // Jednotnou validační chybu vracíme níže.
        }
        throw validationError("role", "Kód lze nastavit pouze pro čtení nebo editaci.");
    }

    private TeamRole parseMemberRole(String roleValue) {
        try {
            return TeamRole.from(roleValue);
        } catch (RuntimeException exception) {
            throw validationError("role", "Vyber platná práva: žádná práva, čtení, editace nebo administrátor.");
        }
    }

    private String normalizeCode(String value, String field) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!CUSTOM_CODE_PATTERN.matcher(normalized).matches()) {
            throw validationError(
                    field,
                    "Kód musí mít 4 až 32 znaků a může obsahovat písmena, čísla, pomlčku a podtržítko."
            );
        }
        return normalized;
    }

    private String displayName(UserEntity user) {
        return user.getName() == null || user.getName().isBlank()
                ? user.getMail()
                : user.getName();
    }

    private FieldValidationException validationError(String field, String message) {
        return new FieldValidationException(message, List.of(new ValidationField(field, message)));
    }
}
