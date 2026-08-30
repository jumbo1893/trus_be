package com.jumbo.trus.config.security.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class FirebaseTokenVerifier {

    private static final String FIREBASE_APP_NAME = "trus-auth";
    private static final String LOCAL_CREDENTIALS_PATH = "src/main/resources/serviceAccountKey.json";

    private volatile FirebaseAuth firebaseAuth;

    public VerifiedFirebaseToken verify(String idToken) throws Exception {
        // Podpis a platnost ID tokenu se ověří lokálně proti cachovaným Google
        // klíčům. Kontrola revokace by pro každý API request přidávala vzdálené
        // volání; explicitně ji lze použít jen u citlivých jednorázových akcí.
        FirebaseToken decoded = getFirebaseAuth().verifyIdToken(idToken);
        return new VerifiedFirebaseToken(
                decoded.getUid(),
                decoded.getEmail(),
                decoded.getName()
        );
    }

    private FirebaseAuth getFirebaseAuth() throws IOException {
        FirebaseAuth current = firebaseAuth;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (firebaseAuth == null) {
                FirebaseApp app = FirebaseApp.getApps().stream()
                        .filter(existing -> FIREBASE_APP_NAME.equals(existing.getName()))
                        .findFirst()
                        .orElseGet(this::initializeFirebaseApp);
                firebaseAuth = FirebaseAuth.getInstance(app);
            }
            return firebaseAuth;
        }
    }

    private FirebaseApp initializeFirebaseApp() {
        try (InputStream credentials = openCredentials()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            return FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Admin credentials could not be loaded", exception);
        }
    }

    private InputStream openCredentials() throws IOException {
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS");
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }
        return new FileInputStream(LOCAL_CREDENTIALS_PATH);
    }
}
