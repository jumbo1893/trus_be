package com.jumbo.trus.config;

import java.time.Instant;
import java.util.Date;

public class Config {
    public static final String ADMIN_USER_NAME = "Admin";
    public static long AUTOMATIC_SEASON_ID = -2;
    public static String AUTOMATIC_SEASON_NAME = "Automaticky přiřadit sezonu";
    public static Date AUTOMATIC_SEASON_DATE = Date.from(Instant.EPOCH);

    public static long ALL_SEASON_ID = -3;
    public static String ALL_SEASON_NAME = "Všechny sezony";
    public static Date ALL_SEASON_DATE = Date.from(Instant.EPOCH);

    public static long OTHER_SEASON_ID = -1;
    public static String OTHER_SEASON_NAME = "Ostatní";
    public static Date OTHER_SEASON_DATE = Date.from(Instant.EPOCH);

    public static long GOAL_FINE_ID = -1;

    public static long HATTRICK_FINE_ID = -2;

}
