package com.jumbo.trus.config.security.firebase;

import java.security.Principal;

public record FirebaseIdentity(
        String uid,
        String email,
        String displayName
) implements Principal {

    @Override
    public String getName() {
        return uid;
    }
}
