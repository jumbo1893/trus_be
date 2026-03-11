package com.jumbo.trus.service.activity.footbar.connect;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class PKCEGenerator {

    public Map<String, String> generatePkcePair() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[random.nextInt(96 - 32 + 1) + 32]; // Pro 43-128 chars po base64
        random.nextBytes(randomBytes);
        String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] digest = sha256.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        Map<String, String> pair = new HashMap<>();
        pair.put("verifier", codeVerifier);
        pair.put("challenge", codeChallenge);
        return pair;
    }
}