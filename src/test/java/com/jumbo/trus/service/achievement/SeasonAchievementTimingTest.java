package com.jumbo.trus.service.achievement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;

class SeasonAchievementTimingTest {
    @ParameterizedTest
    @CsvSource({
        "2026-06-30T21:59:59Z,2026-06-29T21:59:59Z,false",
        "2026-06-30T21:59:59Z,2026-06-29T22:00:00Z,true",
        "2026-06-30T21:59:59Z,2026-07-01T00:00:00Z,true",
        "2026-12-31T22:59:59Z,2026-12-30T22:59:59Z,false",
        "2026-12-31T22:59:59Z,2026-12-30T23:00:00Z,true"
    })
    void includesEntireFinalPragueDayInSummerAndWinter(String end, String now, boolean expected) {
        assertThat(SeasonAchievementTiming.isDue(Date.from(Instant.parse(end)),
                Clock.fixed(Instant.parse(now), ZoneOffset.UTC))).isEqualTo(expected);
    }
}
