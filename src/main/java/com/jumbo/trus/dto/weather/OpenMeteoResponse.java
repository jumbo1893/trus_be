package com.jumbo.trus.dto.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OpenMeteoResponse {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Current current;
    private Hourly hourly;

    @Data
    public static class Current {
        private LocalDateTime time;
        @JsonProperty("temperature_2m") private BigDecimal temperature;
        @JsonProperty("apparent_temperature") private BigDecimal apparentTemperature;
        @JsonProperty("relative_humidity_2m") private Integer relativeHumidity;
        private BigDecimal precipitation;
        private BigDecimal rain;
        private BigDecimal snowfall;
        @JsonProperty("weather_code") private Integer weatherCode;
        @JsonProperty("cloud_cover") private Integer cloudCover;
        @JsonProperty("wind_speed_10m") private BigDecimal windSpeed;
        @JsonProperty("wind_gusts_10m") private BigDecimal windGusts;
        @JsonProperty("wind_direction_10m") private Integer windDirection;
        @JsonProperty("surface_pressure") private BigDecimal surfacePressure;
        @JsonProperty("is_day") private Integer day;
    }

    @Data
    public static class Hourly {
        private List<LocalDateTime> time;
        @JsonProperty("temperature_2m") private List<BigDecimal> temperatures;
        @JsonProperty("apparent_temperature") private List<BigDecimal> apparentTemperatures;
        @JsonProperty("relative_humidity_2m") private List<Integer> relativeHumidities;
        private List<BigDecimal> precipitation;
        private List<BigDecimal> rain;
        private List<BigDecimal> snowfall;
        @JsonProperty("weather_code") private List<Integer> weatherCodes;
        @JsonProperty("cloud_cover") private List<Integer> cloudCover;
        @JsonProperty("wind_speed_10m") private List<BigDecimal> windSpeed;
        @JsonProperty("wind_gusts_10m") private List<BigDecimal> windGusts;
        @JsonProperty("wind_direction_10m") private List<Integer> windDirection;
        @JsonProperty("surface_pressure") private List<BigDecimal> surfacePressure;
        @JsonProperty("is_day") private List<Integer> day;
    }
}
