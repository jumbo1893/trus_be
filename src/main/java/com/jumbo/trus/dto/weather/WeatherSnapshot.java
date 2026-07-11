package com.jumbo.trus.dto.weather;

import com.jumbo.trus.entity.weather.WeatherSourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherSnapshot(
        BigDecimal temperature,
        BigDecimal apparentTemperature,
        Integer relativeHumidity,
        BigDecimal precipitation,
        BigDecimal rain,
        BigDecimal snowfall,
        Integer weatherCode,
        Integer cloudCover,
        BigDecimal windSpeed,
        BigDecimal windGusts,
        Integer windDirection,
        BigDecimal surfacePressure,
        Boolean day,
        LocalDateTime measuredAt,
        BigDecimal latitude,
        BigDecimal longitude,
        String provider,
        WeatherSourceType sourceType
) {
}
