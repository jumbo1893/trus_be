package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.AppTeamRegistration;
import com.jumbo.trus.dto.auth.UserDTO;
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
import com.jumbo.trus.service.header.HeaderManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private final AppTeamService service = new AppTeamService(
            teamRepository,
            userService,
            appTeamRepository,
            userTeamRoleRepository,
            appTeamMapper,
            userTeamRoleMapper,
            headerManager,
            playerRepository
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
    }
}
