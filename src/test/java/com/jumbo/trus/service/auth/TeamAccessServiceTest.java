package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.AppTeamJoinRequest;
import com.jumbo.trus.dto.auth.AppTeamJoinResult;
import com.jumbo.trus.dto.auth.UpdateJoinCodeRequest;
import com.jumbo.trus.dto.auth.UpdateTeamMemberRoleRequest;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.TeamJoinCodeEntity;
import com.jumbo.trus.entity.auth.TeamRole;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.auth.TeamJoinCodeRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.header.HeaderManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamAccessServiceTest {

    private final TeamJoinCodeRepository joinCodeRepository = mock(TeamJoinCodeRepository.class);
    private final UserTeamRoleRepository userTeamRoleRepository = mock(UserTeamRoleRepository.class);
    private final AppTeamRepository appTeamRepository = mock(AppTeamRepository.class);
    private final UserService userService = mock(UserService.class);
    private final HeaderManager headerManager = mock(HeaderManager.class);
    private final TeamAccessService service = new TeamAccessService(
            joinCodeRepository,
            userTeamRoleRepository,
            appTeamRepository,
            userService,
            headerManager
    );

    @Test
    void createsTwoDifferentTenCharacterCodesForNewTeam() {
        AppTeamEntity appTeam = appTeam(10L, "Nový tým");
        AtomicLong ids = new AtomicLong(1);
        when(joinCodeRepository.findByAppTeamIdAndGrantedRole(any(), any()))
                .thenReturn(Optional.empty());
        when(joinCodeRepository.existsByCode(any())).thenReturn(false);
        when(joinCodeRepository.save(any(TeamJoinCodeEntity.class))).thenAnswer(invocation -> {
            TeamJoinCodeEntity code = invocation.getArgument(0);
            code.setId(ids.getAndIncrement());
            return code;
        });

        service.createJoinCodes(appTeam);

        ArgumentCaptor<TeamJoinCodeEntity> captor = ArgumentCaptor.forClass(TeamJoinCodeEntity.class);
        verify(joinCodeRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<TeamJoinCodeEntity> codes = captor.getAllValues();
        assertEquals(List.of(TeamRole.READER, TeamRole.EDITOR),
                codes.stream().map(TeamJoinCodeEntity::getGrantedRole).toList());
        assertTrue(codes.stream().allMatch(code -> code.getCode().length() == 10));
        assertNotEquals(codes.get(0).getCode(), codes.get(1).getCode());
    }

    @Test
    void editorCodeUpgradesExistingReaderAndReturnsJoinedTeam() {
        AppTeamEntity appTeam = appTeam(10L, "Nový tým");
        TeamJoinCodeEntity code = joinCode(20L, "EDIT-CODE", TeamRole.EDITOR, appTeam);
        UserEntity user = user(30L, "Hráč");
        UserTeamRole currentRole = role(40L, user, appTeam, TeamRole.READER);
        UserDTO responseUser = new UserDTO();

        when(joinCodeRepository.findByCode("EDIT-CODE")).thenReturn(Optional.of(code));
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(user.getId(), appTeam.getId()))
                .thenReturn(Optional.of(currentRole));
        when(userService.getCurrentUser()).thenReturn(responseUser);

        AppTeamJoinResult result = service.joinCurrentUser(new AppTeamJoinRequest(" edit-code "));

        assertEquals(TeamRole.EDITOR.name(), currentRole.getRole());
        assertEquals(appTeam.getId(), result.getAppTeamId());
        assertEquals(TeamRole.EDITOR.name(), result.getAssignedRole());
        assertEquals(responseUser, result.getUser());
        verify(userTeamRoleRepository).save(currentRole);
        verify(userService).refreshUserInSecurityContext();
    }

    @Test
    void readerCodeNeverDowngradesAdministrator() {
        AppTeamEntity appTeam = appTeam(10L, "Nový tým");
        TeamJoinCodeEntity code = joinCode(20L, "READ-CODE", TeamRole.READER, appTeam);
        UserEntity user = user(30L, "Admin");
        UserTeamRole currentRole = role(40L, user, appTeam, TeamRole.ADMIN);
        when(joinCodeRepository.findByCode("READ-CODE")).thenReturn(Optional.of(code));
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(user.getId(), appTeam.getId()))
                .thenReturn(Optional.of(currentRole));
        when(userService.getCurrentUser()).thenReturn(new UserDTO());

        service.joinCurrentUser(new AppTeamJoinRequest("READ-CODE"));

        assertEquals(TeamRole.ADMIN.name(), currentRole.getRole());
    }

    @Test
    void refusesCodeAlreadyUsedByAnotherTeam() {
        AppTeamEntity currentTeam = appTeam(10L, "Aktuální tým");
        AppTeamEntity otherTeam = appTeam(11L, "Jiný tým");
        TeamJoinCodeEntity currentCode = joinCode(20L, "CURRENT", TeamRole.READER, currentTeam);
        TeamJoinCodeEntity conflict = joinCode(21L, "MY-TEAM", TeamRole.EDITOR, otherTeam);
        when(headerManager.getAppTeamIdHeader()).thenReturn(currentTeam.getId());
        when(appTeamRepository.findById(currentTeam.getId())).thenReturn(Optional.of(currentTeam));
        when(joinCodeRepository.findByAppTeamIdAndGrantedRole(currentTeam.getId(), TeamRole.READER))
                .thenReturn(Optional.of(currentCode));
        when(joinCodeRepository.findByCode("MY-TEAM")).thenReturn(Optional.of(conflict));

        FieldValidationException exception = assertThrows(
                FieldValidationException.class,
                () -> service.updateJoinCode("reader", new UpdateJoinCodeRequest("my-team"))
        );

        assertEquals("code", exception.getFields().get(0).getField());
    }

    @Test
    void founderCannotLoseAdministratorRole() {
        AppTeamEntity appTeam = appTeam(10L, "Nový tým");
        UserEntity owner = user(30L, "Zakladatel");
        UserEntity currentAdmin = user(31L, "Další admin");
        appTeam.setOwner(owner);
        UserTeamRole ownerRole = role(40L, owner, appTeam, TeamRole.ADMIN);
        when(headerManager.getAppTeamIdHeader()).thenReturn(appTeam.getId());
        when(appTeamRepository.findById(appTeam.getId())).thenReturn(Optional.of(appTeam));
        when(userTeamRoleRepository.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
        when(userService.getCurrentUserEntity()).thenReturn(currentAdmin);

        FieldValidationException exception = assertThrows(
                FieldValidationException.class,
                () -> service.updateMemberRole(
                        ownerRole.getId(),
                        new UpdateTeamMemberRoleRequest(TeamRole.READER.name())
                )
        );

        assertEquals("administrator", exception.getFields().get(0).getField());
        assertEquals(TeamRole.ADMIN.name(), ownerRole.getRole());
    }

    @Test
    void administratorCanSetReaderEditorAndAdministratorRoles() {
        AppTeamEntity appTeam = appTeam(10L, "Nový tým");
        UserEntity currentAdmin = user(30L, "Admin");
        UserEntity member = user(31L, "Člen");
        UserTeamRole memberRole = role(40L, member, appTeam, TeamRole.ADMIN);
        TeamJoinCodeEntity readerCode = joinCode(50L, "READ-CODE", TeamRole.READER, appTeam);
        TeamJoinCodeEntity editorCode = joinCode(51L, "EDIT-CODE", TeamRole.EDITOR, appTeam);
        when(headerManager.getAppTeamIdHeader()).thenReturn(appTeam.getId());
        when(appTeamRepository.findById(appTeam.getId())).thenReturn(Optional.of(appTeam));
        when(userTeamRoleRepository.findById(memberRole.getId())).thenReturn(Optional.of(memberRole));
        when(userTeamRoleRepository.findAllByAppTeamId(appTeam.getId())).thenReturn(List.of(memberRole));
        when(joinCodeRepository.findByAppTeamIdAndGrantedRole(appTeam.getId(), TeamRole.READER))
                .thenReturn(Optional.of(readerCode));
        when(joinCodeRepository.findByAppTeamIdAndGrantedRole(appTeam.getId(), TeamRole.EDITOR))
                .thenReturn(Optional.of(editorCode));
        when(userService.getCurrentUserEntity()).thenReturn(currentAdmin);

        for (TeamRole newRole : List.of(TeamRole.READER, TeamRole.EDITOR, TeamRole.ADMIN)) {
            service.updateMemberRole(
                    memberRole.getId(),
                    new UpdateTeamMemberRoleRequest(newRole.name())
            );
            assertEquals(newRole.name(), memberRole.getRole());
        }

        verify(userTeamRoleRepository, org.mockito.Mockito.times(3)).save(memberRole);
    }

    private AppTeamEntity appTeam(long id, String name) {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(id);
        appTeam.setName(name);
        return appTeam;
    }

    private UserEntity user(long id, String name) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName(name);
        user.setMail(name.toLowerCase() + "@example.cz");
        return user;
    }

    private TeamJoinCodeEntity joinCode(
            long id,
            String value,
            TeamRole role,
            AppTeamEntity appTeam
    ) {
        TeamJoinCodeEntity code = new TeamJoinCodeEntity();
        code.setId(id);
        code.setCode(value);
        code.setGrantedRole(role);
        code.setAppTeam(appTeam);
        return code;
    }

    private UserTeamRole role(
            long id,
            UserEntity user,
            AppTeamEntity appTeam,
            TeamRole teamRole
    ) {
        UserTeamRole role = new UserTeamRole();
        role.setId(id);
        role.setUser(user);
        role.setAppTeam(appTeam);
        role.setRole(teamRole.name());
        return role;
    }
}
