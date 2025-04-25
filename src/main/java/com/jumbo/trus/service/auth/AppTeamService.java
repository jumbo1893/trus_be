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
import com.jumbo.trus.entity.repository.auth.AppTeamRepository;
import com.jumbo.trus.entity.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.entity.repository.football.TeamRepository;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.auth.AppTeamMapper;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.helper.ValidationField;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.List;
import java.util.Optional;

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
    private final PlayerMapper playerMapper;
    private final PlayerService playerService;


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

    public UserDTO registerAppTeam(AppTeamRegistration appTeamRegistration) {
        UserEntity user = userService.getCurrentUserEntity();
        TeamEntity team = teamRepository.findById(appTeamRegistration.getFootballTeamId())
                .orElseThrow(() -> new NotFoundException("Tým s id " + appTeamRegistration.getFootballTeamId() + " nenalezen!"));
        createNewAppTeamIfNotExists(appTeamRegistration, user, team);
        userService.refreshUserInSecurityContext();
        return userService.getCurrentUser();
    }

    public UserDTO addCurrentUserToAppTeam(Long appTeamId) {
        UserEntity user = userService.getCurrentUserEntity();
        AppTeamEntity appTeam = findAppTeamByIdOrThrow(appTeamId);
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
        userTeamRoleRepository.save(userTeamRole);
    }

    public void addPlayerToCurrentUser(UserEntity userEntity, PlayerDTO playerDTO) {
        UserTeamRole userTeamRole = findCurrentTeamRole(userEntity.getTeamRoles());
        if (userTeamRole == null) {
            throw new NotFoundException("Nenalezena role pro user " + userEntity.getUsername());
        }

        PlayerEntity playerEntity = playerService.getPlayerEntity(playerDTO.getId());
        log.debug("footballPlayer id: {}", playerEntity.getFootballPlayer() != null ? playerEntity.getFootballPlayer().getId() : "null");
        userTeamRole.setPlayer(playerEntity); // důležité: žádné toEntity()
        userTeamRoleRepository.save(userTeamRole);
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
        newAppTeam.setName(appTeamRegistration.getName());
        newAppTeam.setOwner(user);
        newAppTeam.setTeam(team);
        return appTeamRepository.save(newAppTeam);
    }

    private void createNewAppTeamIfNotExists(AppTeamRegistration appTeamRegistration, UserEntity user, TeamEntity team) {
        Optional<AppTeamEntity> existingTeam = findAppTeamByName(appTeamRegistration.getName());
        if (existingTeam.isPresent()) {
            List<ValidationField> fields = List.of(
                    new ValidationField("appTeamName", "Jméno " + appTeamRegistration.getName() + " již existuje!")
            );
            throw new FieldValidationException("Dané jméno již existuje", fields);
        }
        createNewUserTeamRole(user, createNewAppTeam(appTeamRegistration, user, team), "ADMIN");
    }
}
