package com.jumbo.trus.config.security;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.TeamRole;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.header.HeaderManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleValidationAspectTest {

    private final HeaderManager headerManager = mock(HeaderManager.class);
    private final AppTeamRepository appTeamRepository = mock(AppTeamRepository.class);
    private final RoleValidationAspect aspect = new RoleValidationAspect();
    private final RoleRequired roleRequired = mock(RoleRequired.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aspect, "headerManager", headerManager);
        ReflectionTestUtils.setField(aspect, "appTeamRepository", appTeamRepository);
        when(headerManager.getAppTeamIdHeader()).thenReturn(12L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void editorCanUseEditorAndReaderEndpoints() {
        authenticateAs(TeamRole.EDITOR);

        when(roleRequired.value()).thenReturn("EDITOR");
        assertDoesNotThrow(() -> aspect.validateRole(roleRequired));
        when(roleRequired.value()).thenReturn("READER");
        assertDoesNotThrow(() -> aspect.validateRole(roleRequired));
    }

    @Test
    void editorCannotUseAdministratorEndpoints() {
        authenticateAs(TeamRole.EDITOR);
        when(roleRequired.value()).thenReturn("ADMIN");

        AuthException exception = assertThrows(
                AuthException.class,
                () -> aspect.validateRole(roleRequired)
        );
        assertTrue(exception.getMessage().contains("Testovací tým"));
        assertTrue(exception.getMessage().contains("pouze administrátoři"));
        assertTrue(exception.getMessage().contains("čtení a editace"));
        assertTrue(!exception.getMessage().contains("12"));
    }

    @Test
    void legacyAdministratorStillHasEveryTeamPermission() {
        authenticateAs(TeamRole.ADMIN);

        for (TeamRole required : TeamRole.values()) {
            when(roleRequired.value()).thenReturn(required.name());
            assertDoesNotThrow(() -> aspect.validateRole(roleRequired));
        }
    }

    private void authenticateAs(TeamRole teamRole) {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(12L);
        appTeam.setName("Testovací tým");
        UserEntity user = new UserEntity();
        user.setId(7L);
        UserTeamRole role = new UserTeamRole();
        role.setAppTeam(appTeam);
        role.setUser(user);
        role.setRole(teamRole.name());
        user.setTeamRoles(List.of(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }
}
