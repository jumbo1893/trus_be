package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.AppTeamDTO;
import com.jumbo.trus.dto.auth.AppTeamRegistration;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.auth.UserTeamRoleDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.mapper.auth.AppTeamMapper;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.football.TeamRepository;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.header.HeaderManager;
import com.jumbo.trus.service.helper.ValidationField;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppTeamService implements AppTeamProvider {

    private final TeamRepository teamRepository;
    private final UserService userService;
    private final AppTeamRepository appTeamRepository;
    private final UserTeamRoleRepository userTeamRoleRepository;
    private final AppTeamMapper appTeamMapper;
    private final UserTeamRoleMapper userTeamRoleMapper;
    private final HeaderManager headerManager;
    private final PlayerRepository playerRepository;


    public AppTeamEntity getCurrentAppTeamOrThrow() {
        Long id = headerManager.getAppTeamIdHeader();
        if (id == null) {
            throw new AuthException("Pro tuto operaci je třeba uvést ID týmu v hlavičce!", AuthException.MISSING_TEAM_ID);
        }
        return findAppTeamByIdOrThrow(id);
    }

    public List<AppTeamDTO> getAllAppTeams() {
        return appTeamRepository.findAll().stream().map(appTeamMapper::toDTO).toList();
    }

    @Transactional
    public UserDTO registerAppTeam(AppTeamRegistration appTeamRegistration) {
        UserEntity user = userService.getCurrentUserEntity();
        validateAppTeamName(appTeamRegistration.getName());
        TeamEntity team = appTeamRegistration.getFootballTeamId() == null
                ? createStandaloneTeam(appTeamRegistration.getName().trim())
                : teamRepository.findById(appTeamRegistration.getFootballTeamId())
                    .orElseThrow(() -> new NotFoundException("Tým s id " + appTeamRegistration.getFootballTeamId() + " nenalezen!"));
        createNewAppTeamIfNotExists(appTeamRegistration, user, team);
        userService.refreshUserInSecurityContext();
        return userService.getCurrentUser();
    }

    @Transactional
    public UserDTO addCurrentUserToAppTeam(Long appTeamId) {
        UserEntity user = userService.getCurrentUserEntity();
        AppTeamEntity appTeam = findAppTeamByIdOrThrow(appTeamId);
        if (userTeamRoleRepository.findByUserIdAndAppTeamId(user.getId(), appTeamId).isPresent()) {
            return userService.getCurrentUser();
        }
        createNewUserTeamRole(user, appTeam, "READER");
        userService.refreshUserInSecurityContext();
        return userService.getCurrentUser();
    }

    public AppTeamDTO getLisciTrusAppTeam() {
        return appTeamMapper.toDTO(findAppTeamByName("Liščí Trus").orElseThrow());
    }

    private Optional<AppTeamEntity> findAppTeamByName(String name) {
        return appTeamRepository.findByName(name);
    }

    private Optional<AppTeamEntity> findAppTeamById(Long id) {
        return appTeamRepository.findById(id);
    }

    public AppTeamEntity findAppTeamByIdOrThrow(Long id) {
        return findAppTeamById(id)
                .orElseThrow(() -> new NotFoundException("App team s id " + id + " nenalezen v db"));
    }

    private void createNewUserTeamRole(UserEntity user, AppTeamEntity appTeam, String role) {
        UserTeamRole userTeamRole = new UserTeamRole();
        userTeamRole.setUser(user);
        userTeamRole.setAppTeam(appTeam);
        userTeamRole.setRole(role);
        UserTeamRole savedRole = userTeamRoleRepository.save(userTeamRole);
        user.getTeamRoles().add(savedRole);
        appTeam.getTeamRoles().add(savedRole);
    }

    @Transactional
    public void addPlayerToCurrentUser(UserEntity userEntity, PlayerDTO playerDTO) {
        AppTeamEntity appTeam = getCurrentAppTeamOrThrow();
        UserTeamRole userTeamRole = userTeamRoleRepository
                .findByUserIdAndAppTeamId(userEntity.getId(), appTeam.getId())
                .orElseThrow(() -> new NotFoundException("Nenalezena role pro user " + userEntity.getUsername()));

        if (playerDTO.equals(PlayerService.noPlayer())) {
            userTeamRole.setPlayer(null);
            userTeamRoleRepository.save(userTeamRole);
            return;
        }

        pairPlayerToRole(userTeamRole, userEntity.getId(), playerDTO.getId(), appTeam);
    }

    /**
     * Jediné místo, které smí vytvořit vazbu uživatel–hráč. Díky zámku nad
     * hráčem platí kontrola i pro souběžné požadavky z různých obrazovek.
     */
    @Transactional
    public PlayerEntity pairPlayerToRole(
            UserTeamRole userTeamRole,
            Long userId,
            Long playerId,
            AppTeamEntity appTeam
    ) {

        // U již existující (i historicky duplicitní) vazby nejde o nové spárování.
        if (userTeamRole.getPlayer() != null
                && userTeamRole.getPlayer().getId() == playerId
                && !userTeamRole.getPlayer().isDeleted()
                && userTeamRole.getPlayer().getAppTeam() != null
                && appTeam.getId().equals(userTeamRole.getPlayer().getAppTeam().getId())) {
            return userTeamRole.getPlayer();
        }

        PlayerEntity playerEntity = playerRepository.findByIdForUpdate(playerId)
                .filter(player -> !player.isDeleted())
                .filter(player -> player.getAppTeam() != null
                        && appTeam.getId().equals(player.getAppTeam().getId()))
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(playerId)));

        List<UserTeamRole> conflictingAssignments = userTeamRoleRepository.findPlayerAssignmentsOfOtherUsers(
                appTeam.getId(),
                playerEntity.getId(),
                userId
        );
        if (!conflictingAssignments.isEmpty()) {
            UserEntity pairedUser = conflictingAssignments.get(0).getUser();
            String pairedUserName = pairedUser.getName();
            if (pairedUserName == null || pairedUserName.isBlank()) {
                pairedUserName = pairedUser.getMail();
            }
            String message = "Tento hráč je již spárovaný s uživatelem " + pairedUserName + ".";
            throw new FieldValidationException(
                    message,
                    List.of(new ValidationField("player", message))
            );
        }

        userTeamRole.setPlayer(playerEntity);
        userTeamRoleRepository.save(userTeamRole);
        return playerEntity;
    }

    public PlayerEntity getPlayerEntity(long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(playerId)));
    }


    public UserTeamRoleDTO findCurrentTeamRoleByUserId(Long userId) {
        UserEntity userEntity = userService.findById(userId);
        return userTeamRoleMapper.toDTO(findCurrentTeamRole(userEntity.getTeamRoles()));
    }

    public void changeUserRole(Long userRoleId, String role) {
        UserTeamRole userTeamRole = userTeamRoleRepository.findById(userRoleId).orElseThrow(() -> new NotFoundException("Role pro userRoleId " + userRoleId + " nenalezena!"));
        userTeamRole.setRole(role);
        userTeamRoleRepository.save(userTeamRole);
    }

    private UserTeamRole findCurrentTeamRole(List<UserTeamRole> userTeamRoles) {
        AppTeamEntity appTeam = getCurrentAppTeamOrThrow();
        for (UserTeamRole userTeamRole : userTeamRoles) {
            if (userTeamRole.getAppTeam().equals(appTeam)) {
                return userTeamRole;
            }
        }
        return null;
    }

    private AppTeamEntity createNewAppTeam(AppTeamRegistration appTeamRegistration, UserEntity user, TeamEntity team) {
        AppTeamEntity newAppTeam = new AppTeamEntity();
        newAppTeam.setName(appTeamRegistration.getName().trim());
        newAppTeam.setOwner(user);
        newAppTeam.setTeam(team);
        return appTeamRepository.save(newAppTeam);
    }

    private void createNewAppTeamIfNotExists(AppTeamRegistration appTeamRegistration, UserEntity user, TeamEntity team) {
        Optional<AppTeamEntity> existingTeam = appTeamRepository.findByNameIgnoreCase(appTeamRegistration.getName().trim());
        if (existingTeam.isPresent()) {
            List<ValidationField> fields = List.of(
                    new ValidationField("appTeamName", "Jméno " + appTeamRegistration.getName() + " již existuje!")
            );
            throw new FieldValidationException("Dané jméno již existuje", fields);
        }
        createNewUserTeamRole(user, createNewAppTeam(appTeamRegistration, user, team), "ADMIN");
    }

    private TeamEntity createStandaloneTeam(String name) {
        TeamEntity team = new TeamEntity();
        team.setName(name);
        team.setUri("custom:" + UUID.randomUUID());
        return teamRepository.save(team);
    }

    private void validateAppTeamName(String name) {
        if (name == null || name.isBlank()) {
            throw new FieldValidationException(
                    "Vyplň název týmu",
                    List.of(new ValidationField("appTeamName", "Vyplň název týmu"))
            );
        }
    }
}
