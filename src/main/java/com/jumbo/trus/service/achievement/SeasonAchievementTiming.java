package com.jumbo.trus.service.achievement;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public final class SeasonAchievementTiming {
    public static final ZoneId ZONE = ZoneId.of("Europe/Prague");
    private SeasonAchievementTiming() { }
    public static boolean isDue(Date end, Clock clock) {
        return end != null && !Instant.ofEpochMilli(end.getTime()).atZone(ZONE).toLocalDate()
                .isAfter(LocalDate.now(clock.withZone(ZONE)));
    }
}
