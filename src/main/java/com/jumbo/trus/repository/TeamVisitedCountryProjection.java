package com.jumbo.trus.repository;

import java.time.LocalDateTime;

public interface TeamVisitedCountryProjection {

    Long getPlayerId();

    String getPlayerName();

    String getCode();

    String getNameCs();

    LocalDateTime getFirstVisitedAt();

    String getContinentCode();
}
