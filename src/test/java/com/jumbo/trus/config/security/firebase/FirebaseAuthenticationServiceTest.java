package com.jumbo.trus.config.security.firebase;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebaseAuthenticationServiceTest {

    private final FirebaseTokenVerifier verifier = mock(FirebaseTokenVerifier.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FirebaseAuthenticationService service =
            new FirebaseAuthenticationService(verifier, userRepository);

    @Test
    void linksExistingLegacyUserByNormalizedEmail() throws Exception {
        UserEntity legacyUser = new UserEntity();
        legacyUser.setId(7L);
        legacyUser.setMail("player@example.com");
        when(verifier.verify("token")).thenReturn(
                new VerifiedFirebaseToken("firebase-7", " PLAYER@example.com ", "Player")
        );
        when(userRepository.findByFirebaseUid("firebase-7")).thenReturn(Optional.empty());
        when(userRepository.findWithTeamRolesByMailIgnoreCase("player@example.com"))
                .thenReturn(Optional.of(legacyUser));
        when(userRepository.saveAndFlush(legacyUser)).thenReturn(legacyUser);

        Authentication authentication = service.authenticate("token");

        assertSame(legacyUser, authentication.getPrincipal());
        assertEquals("firebase-7", legacyUser.getFirebaseUid());
        verify(userRepository).saveAndFlush(legacyUser);
    }

    @Test
    void createsTemporaryIdentityUntilNewUserIsProvisioned() throws Exception {
        when(verifier.verify("token")).thenReturn(
                new VerifiedFirebaseToken("firebase-new", "new@example.com", "New")
        );
        when(userRepository.findByFirebaseUid("firebase-new")).thenReturn(Optional.empty());
        when(userRepository.findWithTeamRolesByMailIgnoreCase("new@example.com"))
                .thenReturn(Optional.empty());

        Authentication authentication = service.authenticate("token");

        FirebaseIdentity identity = assertInstanceOf(
                FirebaseIdentity.class,
                authentication.getPrincipal()
        );
        assertEquals("firebase-new", identity.uid());
        assertEquals("new@example.com", identity.email());
    }
}
