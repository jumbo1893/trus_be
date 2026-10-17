package com.jumbo.trus.service.fine;

import java.util.Set;

public final class FineCategories {
    public static final String FREQUENT = "FREQUENT";
    public static final String COMMON = "COMMON";
    public static final String RARE = "RARE";
    public static final String AUTOMATIC = "AUTOMATIC";

    private static final Set<String> AUTOMATIC_CODES = Set.of(
            FineCodes.GOAL, FineCodes.HATTRICK, FineCodes.ZERO_IN_MATCH, FineCodes.THIRD_HALF);

    private FineCategories() {}

    public static boolean isAutomatic(String code) {
        return code != null && AUTOMATIC_CODES.contains(code);
    }
}
