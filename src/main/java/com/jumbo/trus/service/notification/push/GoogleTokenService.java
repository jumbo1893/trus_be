package com.jumbo.trus.service.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class GoogleTokenService {

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    public String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials;

        // Zkus načíst klíč z ENV proměnné
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS");

        if (credentialsJson != null && !credentialsJson.isBlank()) {
            googleCredentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of(SCOPE));
        } else {
            // fallback pro lokální vývoj
            googleCredentials = GoogleCredentials
                    .fromStream(new FileInputStream("src/main/resources/serviceAccountKey.json"))
                    .createScoped(List.of(SCOPE));
        }

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}
