package com.jumbo.trus.entity.auth;

import java.util.Locale;

public enum TeamRole {
    ADMIN,
    EDITOR,
    READER;

    public static TeamRole from(String value) {
        return TeamRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean hasAtLeast(TeamRole requiredRole) {
        return ordinal() <= requiredRole.ordinal();
    }
}
