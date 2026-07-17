package com.jumbo.trus.entity;

import com.jumbo.trus.entity.weather.WeatherSourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_weather")
@Getter
@Setter
@NoArgsConstructor
public class MatchWeatherEntity {

    @Id
    @Column(name = "match_id")
    private Long matchId;

    private Long footballMatchId;

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_match_weather_match"))
    private MatchEntity match;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "apparent_temperature", precision = 5, scale = 2)
    private BigDecimal apparentTemperature;

    @Column(name = "relative_humidity")
    private Integer relativeHumidity;

    @Column(precision = 7, scale = 2)
    private BigDecimal precipitation;

    @Column(precision = 7, scale = 2)
    private BigDecimal rain;

    @Column(precision = 7, scale = 2)
    private BigDecimal snowfall;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "cloud_cover")
    private Integer cloudCover;

    @Column(name = "wind_speed", precision = 7, scale = 2)
    private BigDecimal windSpeed;

    @Column(name = "wind_gusts", precision = 7, scale = 2)
    private BigDecimal windGusts;

    @Column(name = "wind_direction")
    private Integer windDirection;

    @Column(name = "surface_pressure", precision = 7, scale = 2)
    private BigDecimal surfacePressure;

    @Column(name = "is_day")
    private Boolean day;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false, length = 50)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private WeatherSourceType sourceType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
