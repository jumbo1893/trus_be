package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.AppTeamRegistration;
import com.jumbo.trus.dto.auth.UserDTO;
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
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.header.HeaderManager;
import com.jumbo.trus.service.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTeamServiceTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final UserService userService = mock(UserService.class);
    private final AppTeamRepository appTeamRepository = mock(AppTeamRepository.class);
    private final UserTeamRoleRepository userTeamRoleRepository = mock(UserTeamRoleRepository.class);
    private final AppTeamMapper appTeamMapper = mock(AppTeamMapper.class);
    private final UserTeamRoleMapper userTeamRoleMapper = mock(UserTeamRoleMapper.class);
    private final HeaderManager headerManager = mock(HeaderManager.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final TeamAccessService teamAccessService = mock(TeamAccessService.class);
    private final AppTeamService service = new AppTeamService(
            teamRepository,
            userService,
            appTeamRepository,
            userTeamRoleRepository,
            appTeamMapper,
            userTeamRoleMapper,
            headerManager,
            playerRepository,
            teamAccessService
    );

    @Test
    void createsStandaloneTeamWithoutPkflLink() {
        UserEntity owner = new UserEntity();
        owner.setId(7L);
        UserDTO response = new UserDTO();
        AppTeamRegistration registration = new AppTeamRegistration("  Vlastní tým  ", null);

        when(userService.getCurrentUserEntity()).thenReturn(owner);
        when(appTeamRepository.findByNameIgnoreCase("Vlastní tým")).thenReturn(Optional.empty());
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> {
            TeamEntity team = invocation.getArgument(0);
            team.setId(11L);
            return team;
        });
        when(appTeamRepository.save(any(AppTeamEntity.class))).thenAnswer(invocation -> {
            AppTeamEntity appTeam = invocation.getArgument(0);
            appTeam.setId(12L);
            return appTeam;
        });
        when(userTeamRoleRepository.save(any(UserTeamRole.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.getCurrentUser()).thenReturn(response);

        assertSame(response, service.registerAppTeam(registration));

        ArgumentCaptor<TeamEntity> teamCaptor = ArgumentCaptor.forClass(TeamEntity.class);
        verify(teamRepository).save(teamCaptor.capture());
        assertEquals("Vlastní tým", teamCaptor.getValue().getName());
        assertTrue(teamCaptor.getValue().getUri().startsWith("custom:"));
        assertNull(teamCaptor.getValue().getCurrentLeague());
        verify(teamRepository, never()).findById(any());

        ArgumentCaptor<UserTeamRole> roleCaptor = ArgumentCaptor.forClass(UserTeamRole.class);
        verify(userTeamRoleRepository).save(roleCaptor.capture());
        assertSame(owner, roleCaptor.getValue().getUser());
        assertEquals("ADMIN", roleCaptor.getValue().getRole());
        assertEquals("Vlastní tým", roleCaptor.getValue().getAppTeam().getName());
        assertSame(teamCaptor.getValue(), roleCaptor.getValue().getAppTeam().getTeam());
        assertTrue(owner.getTeamRoles().contains(roleCaptor.getValue()));
        assertTrue(roleCaptor.getValue().getAppTeam().getTeamRoles().contains(roleCaptor.getValue()));
        verify(teamAccessService).createJoinCodes(roleCaptor.getValue().getAppTeam());
    }

    @Test
    void legacyAddEndpointRefusesPrivateTeamId() {
        AppTeamEntity publicTeam = appTeam(1L);
        AppTeamEntity privateTeam = appTeam(99L);
        when(appTeamRepository.findByName(AppTeamService.PUBLIC_TEAM_NAME))
                .thenReturn(Optional.of(publicTeam));

        FieldValidationException exception = assertThrows(
                FieldValidationException.class,
                () -> service.addCurrentUserToAppTeam(privateTeam.getId())
        );

        assertEquals("appTeam", exception.getFields().get(0).getField());
        verify(userTeamRoleRepository, never()).save(any());
    }

    @Test
    void newPublicJoinAlwaysUsesLisciTrus() {
        UserEntity user = user(7L, "Matěj", "matej@example.cz");
        AppTeamEntity publicTeam = appTeam(1L);
        UserDTO response = new UserDTO();
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(appTeamRepository.findByName(AppTeamService.PUBLIC_TEAM_NAME))
                .thenReturn(Optional.of(publicTeam));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(user.getId(), publicTeam.getId()))
                .thenReturn(Optional.empty());
        when(userTeamRoleRepository.save(any(UserTeamRole.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.getCurrentUser()).thenReturn(response);

        assertSame(response, service.addCurrentUserToPublicAppTeam());

        ArgumentCaptor<UserTeamRole> roleCaptor = ArgumentCaptor.forClass(UserTeamRole.class);
        verify(userTeamRoleRepository).save(roleCaptor.capture());
        assertEquals("READER", roleCaptor.getValue().getRole());
        assertSame(publicTeam, roleCaptor.getValue().getAppTeam());
    }

    @Test
    void refusesToPairPlayerAlreadyAssignedToAnotherUser() {
        AppTeamEntity appTeam = appTeam(12L);
        UserEntity currentUser = user(7L, "Matěj", "matej@example.cz");
        UserTeamRole currentRole = role(21L, currentUser, appTeam, null);
        PlayerEntity player = player(31L, appTeam);
        UserEntity pairedUser = user(8L, "Petr", "petr@example.cz");
        UserTeamRole conflictingRole = role(22L, pairedUser, appTeam, player);

        when(headerManager.getAppTeamIdHeader()).thenReturn(appTeam.getId());
        when(appTeamRepository.findById(appTeam.getId())).thenReturn(Optional.of(appTeam));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(currentUser.getId(), appTeam.getId()))
                .thenReturn(Optional.of(currentRole));
        when(playerRepository.findByIdForUpdate(player.getId())).thenReturn(Optional.of(player));
        when(userTeamRoleRepository.findPlayerAssignmentsOfOtherUsers(
                appTeam.getId(), player.getId(), currentUser.getId()))
                .thenReturn(List.of(conflictingRole));

        FieldValidationException exception = assertThrows(
                FieldValidationException.class,
                () -> service.addPlayerToCurrentUser(currentUser, playerDto(player.getId()))
        );

        assertEquals("Tento hráč je již spárovaný s uživatelem Petr.", exception.getMessage());
        assertEquals("player", exception.getFields().get(0).getField());
        assertNull(currentRole.getPlayer());
        verify(userTeamRoleRepository, never()).save(currentRole);
    }

    @Test
    void keepsExistingPairingEvenWhenHistoricalDuplicateExists() {
        AppTeamEntity appTeam = appTeam(12L);
        UserEntity currentUser = user(7L, "Matěj", "matej@example.cz");
        PlayerEntity player = player(31L, appTeam);
        UserTeamRole currentRole = role(21L, currentUser, appTeam, player);

        when(headerManager.getAppTeamIdHeader()).thenReturn(appTeam.getId());
        when(appTeamRepository.findById(appTeam.getId())).thenReturn(Optional.of(appTeam));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(currentUser.getId(), appTeam.getId()))
                .thenReturn(Optional.of(currentRole));

        service.addPlayerToCurrentUser(currentUser, playerDto(player.getId()));

        assertSame(player, currentRole.getPlayer());
        verify(playerRepository, never()).findByIdForUpdate(anyLong());
        verify(userTeamRoleRepository, never()).findPlayerAssignmentsOfOtherUsers(anyLong(), anyLong(), anyLong());
        verify(userTeamRoleRepository, never()).save(any());
    }

    @Test
    void allowsUserToRemovePlayerPairing() {
        AppTeamEntity appTeam = appTeam(12L);
        UserEntity currentUser = user(7L, "Matěj", "matej@example.cz");
        PlayerEntity player = player(31L, appTeam);
        UserTeamRole currentRole = role(21L, currentUser, appTeam, player);

        when(headerManager.getAppTeamIdHeader()).thenReturn(appTeam.getId());
        when(appTeamRepository.findById(appTeam.getId())).thenReturn(Optional.of(appTeam));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(currentUser.getId(), appTeam.getId()))
                .thenReturn(Optional.of(currentRole));

        service.addPlayerToCurrentUser(currentUser, PlayerService.noPlayer());

        assertNull(currentRole.getPlayer());
        verify(userTeamRoleRepository).save(currentRole);
        verify(playerRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void pairsAvailablePlayer() {
        AppTeamEntity appTeam = appTeam(12L);
        UserEntity currentUser = user(7L, "Matěj", "matej@example.cz");
        UserTeamRole currentRole = role(21L, currentUser, appTeam, null);
        PlayerEntity player = player(31L, appTeam);

        when(headerManager.getAppTeamIdHeader()).thenReturn(appTeam.getId());
        when(appTeamRepository.findById(appTeam.getId())).thenReturn(Optional.of(appTeam));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(currentUser.getId(), appTeam.getId()))
                .thenReturn(Optional.of(currentRole));
        when(playerRepository.findByIdForUpdate(player.getId())).thenReturn(Optional.of(player));
        when(userTeamRoleRepository.findPlayerAssignmentsOfOtherUsers(
                appTeam.getId(), player.getId(), currentUser.getId()))
                .thenReturn(List.of());

        service.addPlayerToCurrentUser(currentUser, playerDto(player.getId()));

        assertSame(player, currentRole.getPlayer());
        verify(userTeamRoleRepository).save(currentRole);
    }

    @Test
    void refusesToPairPlayerFromAnotherAppTeam() {
        AppTeamEntity currentAppTeam = appTeam(1000L);
        AppTeamEntity anotherAppTeam = appTeam(2000L);
        UserEntity currentUser = user(7L, "Matěj", "matej@example.cz");
        UserTeamRole currentRole = role(21L, currentUser, currentAppTeam, null);
        PlayerEntity player = player(31L, anotherAppTeam);

        when(headerManager.getAppTeamIdHeader()).thenReturn(currentAppTeam.getId());
        when(appTeamRepository.findById(currentAppTeam.getId())).thenReturn(Optional.of(currentAppTeam));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(currentUser.getId(), currentAppTeam.getId()))
                .thenReturn(Optional.of(currentRole));
        when(playerRepository.findByIdForUpdate(player.getId())).thenReturn(Optional.of(player));

        assertThrows(
                jakarta.persistence.EntityNotFoundException.class,
                () -> service.addPlayerToCurrentUser(currentUser, playerDto(player.getId()))
        );

        assertNull(currentRole.getPlayer());
        verify(userTeamRoleRepository, never()).findPlayerAssignmentsOfOtherUsers(anyLong(), anyLong(), anyLong());
        verify(userTeamRoleRepository, never()).save(any());
    }

    private AppTeamEntity appTeam(long id) {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(id);
        return appTeam;
    }

    private UserEntity user(long id, String name, String mail) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName(name);
        user.setMail(mail);
        return user;
    }

    private UserTeamRole role(long id, UserEntity user, AppTeamEntity appTeam, PlayerEntity player) {
        UserTeamRole role = new UserTeamRole();
        role.setId(id);
        role.setUser(user);
        role.setAppTeam(appTeam);
        role.setRole("READER");
        role.setPlayer(player);
        return role;
    }

    private PlayerEntity player(long id, AppTeamEntity appTeam) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setName("Hráč");
        player.setAppTeam(appTeam);
        return player;
    }

    private PlayerDTO playerDto(long id) {
        PlayerDTO player = new PlayerDTO();
        player.setId(id);
        return player;
    }
}
