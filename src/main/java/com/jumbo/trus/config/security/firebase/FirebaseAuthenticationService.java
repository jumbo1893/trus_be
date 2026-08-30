package com.jumbo.trus.config.security.firebase;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FirebaseAuthenticationService {

    private final FirebaseTokenVerifier tokenVerifier;
    private final UserRepository userRepository;

    @Transactional
    public Authentication authenticate(String idToken) throws Exception {
        VerifiedFirebaseToken token = tokenVerifier.verify(idToken);
        if (token.uid() == null || token.uid().isBlank()) {
            throw new IllegalArgumentException("Firebase token does not contain a uid");
        }

        UserEntity user = userRepository.findByFirebaseUid(token.uid()).orElse(null);
        if (user == null && token.email() != null && !token.email().isBlank()) {
            String normalizedEmail = token.email().trim().toLowerCase(Locale.ROOT);
            user = userRepository.findWithTeamRolesByMailIgnoreCase(normalizedEmail).orElse(null);
            if (user != null) {
                if (user.getFirebaseUid() != null && !user.getFirebaseUid().equals(token.uid())) {
                    throw new IllegalStateException("The email is already linked to another Firebase account");
                }
                user.setFirebaseUid(token.uid());
                user = userRepository.saveAndFlush(user);
            }
        }

        if (user != null) {
            return UsernamePasswordAuthenticationToken.authenticated(
                    user,
                    null,
                    user.getAuthorities()
            );
        }

        FirebaseIdentity identity = new FirebaseIdentity(token.uid(), token.email(), token.name());
        return UsernamePasswordAuthenticationToken.authenticated(identity, null, List.of());
    }
}
