package com.jumbo.trus.dto.weather;

import com.jumbo.trus.entity.weather.WeatherSourceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MatchWeatherDTO {
    private BigDecimal temperature;
    private BigDecimal apparentTemperature;
    private Integer relativeHumidity;
    private BigDecimal precipitation;
    private BigDecimal rain;
    private BigDecimal snowfall;
    private Integer weatherCode;
    private Integer cloudCover;
    private BigDecimal windSpeed;
    private BigDecimal windGusts;
    private Integer windDirection;
    private BigDecimal surfacePressure;
    private Boolean day;
    private LocalDateTime measuredAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String provider;
    private WeatherSourceType sourceType;
    private LocalDateTime createdAt;
}
