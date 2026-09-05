package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.MatchWeatherEntity;
import com.jumbo.trus.entity.weather.WeatherSourceType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

class WeatherAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @ParameterizedTest
    @CsvSource({
        "NAVSTEVA_SAHARY,34.99,true,false", "NAVSTEVA_SAHARY,35,true,false", "NAVSTEVA_SAHARY,35.01,true,true",
        "NAVSTEVA_SAHARY,36,false,false", "LEDOVY_MUZ,-0.01,true,true", "LEDOVY_MUZ,0,true,false",
        "LEDOVY_MUZ,0.01,true,false", "LEDOVY_MUZ,-1,false,false"
    })
    void temperatureIsStrictAndRequiresAttendance(String code, BigDecimal temperature, boolean attends, boolean expected) {
        if (!attends) match.setPlayerList(new ArrayList<>());
        MatchWeatherEntity weather = new MatchWeatherEntity(); weather.setMatch(match);
        weather.setTemperature(temperature); weather.setApparentTemperature(new BigDecimal("99"));
        weather.setMeasuredAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        weather.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        weather.setLatitude(BigDecimal.ZERO); weather.setLongitude(BigDecimal.ZERO);
        weather.setProvider("test"); weather.setSourceType(WeatherSourceType.values()[0]); save(weather);
        assertMatch(code, expected);
    }
}
