package com.jumbo.trus.config;

import java.time.Instant;
import java.util.Date;

public class Config {
    public static final String ADMIN_USER_NAME = "Admin";
    public static final long AUTOMATIC_SEASON_ID = -2;
    public static final String AUTOMATIC_SEASON_NAME = "Automaticky přiřadit sezonu";
    public static final Date AUTOMATIC_SEASON_DATE = Date.from(Instant.EPOCH);

    public static final long ALL_SEASON_ID = -3;
    public static final String ALL_SEASON_NAME = "Všechny sezony";
    public static final Date ALL_SEASON_DATE = Date.from(Instant.EPOCH);

    public static final long OTHER_SEASON_ID = -1;
    public static final String OTHER_SEASON_NAME = "Ostatní";
    public static final Date OTHER_SEASON_DATE = Date.from(Instant.EPOCH);

    public static final long GOAL_FINE_ID = -1;

    public static final long HATTRICK_FINE_ID = -2;

}
