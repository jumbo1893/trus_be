package com.jumbo.trus.service.auth;

import com.jumbo.trus.config.security.firebase.FirebaseIdentity;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.service.exceptions.DuplicateEmailException;
import com.jumbo.trus.service.football.team.TeamProcessor;
import com.jumbo.trus.service.membership.MembershipService;
import com.jumbo.trus.service.notification.push.DeviceTokenCollector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserTeamRoleMapper userTeamRoleMapper = mock(UserTeamRoleMapper.class);
    private final TeamProcessor teamProcessor = mock(TeamProcessor.class);
    private final DeviceTokenCollector deviceTokenCollector = mock(DeviceTokenCollector.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerMapper playerMapper = mock(PlayerMapper.class);
    private final MembershipService membershipService = mock(MembershipService.class);
    private final UserService service = new UserService(
            userRepository,
            passwordEncoder,
            userTeamRoleMapper,
            teamProcessor,
            deviceTokenCollector,
            playerRepository,
            playerMapper,
            membershipService
    );

    @Test
    void createsUserAndMembershipFromTheSameManagedEntity() {
        UserDTO request = new UserDTO();
        request.setMail("  PLAYER@EXAMPLE.COM ");
        request.setPassword("firebase-uid");
        request.setName("  Player  ");

        when(passwordEncoder.encode("firebase-uid")).thenReturn("encoded-uid");
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        UserDTO created = service.create(request);

        assertEquals(42L, created.getId());
        assertEquals("player@example.com", created.getMail());
        ArgumentCaptor<UserEntity> savedUser = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        verify(membershipService).initializeBaselineForNewUser(savedUser.getValue());

        assertNotNull(savedUser.getValue().getDeviceTokens());
        assertNotNull(savedUser.getValue().getEnabledPushNotifications());
        assertNotNull(savedUser.getValue().getTeamRoles());
    }

    @Test
    void duplicateEmailDoesNotAttemptToCreateMembership() {
        UserDTO request = new UserDTO();
        request.setMail("player@example.com");
        request.setPassword("firebase-uid");
        request.setName("Player");

        when(passwordEncoder.encode("firebase-uid")).thenReturn("encoded-uid");
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateEmailException.class, () -> service.create(request));
        verify(membershipService, never()).initializeBaselineForNewUser(any(UserEntity.class));
    }

    @Test
    void provisionsNewUserFromVerifiedFirebaseIdentity() {
        FirebaseIdentity identity = new FirebaseIdentity(
                "firebase-42",
                " PLAYER@EXAMPLE.COM ",
                "Firebase name"
        );
        when(userRepository.findByFirebaseUid("firebase-42")).thenReturn(Optional.empty());
        when(userRepository.findByMailIgnoreCase("player@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("internal-password");
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(43L);
            return saved;
        });

        UserDTO created = service.provisionFirebaseUser(identity, "  Player  ");

        ArgumentCaptor<UserEntity> savedUser = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        assertEquals(43L, created.getId());
        assertEquals("firebase-42", savedUser.getValue().getFirebaseUid());
        assertEquals("player@example.com", savedUser.getValue().getMail());
        assertEquals("Player", savedUser.getValue().getName());
        verify(membershipService).initializeBaselineForNewUser(savedUser.getValue());
    }
}
