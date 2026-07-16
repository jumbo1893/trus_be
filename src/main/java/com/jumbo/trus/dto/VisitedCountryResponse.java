package com.jumbo.trus.dto;

import java.time.LocalDateTime;

public record VisitedCountryResponse(
        String code,
        String nameCs,
        LocalDateTime firstVisitedAt,
        String continentCode
        ) {

    private static final String HOME_COUNTRY_CODE = "CZ";

    public boolean isForeignCountry() {
        return code != null
                && !HOME_COUNTRY_CODE.equalsIgnoreCase(code);
    }

    public boolean isInContinent(String expectedContinentCode) {
        return continentCode != null
                && continentCode.equalsIgnoreCase(expectedContinentCode);
    }

}