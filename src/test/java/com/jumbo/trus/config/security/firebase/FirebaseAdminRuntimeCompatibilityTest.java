package com.jumbo.trus.config.security.firebase;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FirebaseAdminRuntimeCompatibilityTest {

    @Test
    void initializesFirebaseTransportWithRuntimeHttpClientVersion() {
        GoogleCredentials credentials = GoogleCredentials.create(
                new AccessToken("test-token", Date.from(Instant.now().plusSeconds(60)))
        );

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        assertNotNull(options);
    }
}
