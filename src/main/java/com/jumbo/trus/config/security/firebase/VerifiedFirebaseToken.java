package com.jumbo.trus.config.security.firebase;

public record VerifiedFirebaseToken(
        String uid,
        String email,
        String name
) {
}
