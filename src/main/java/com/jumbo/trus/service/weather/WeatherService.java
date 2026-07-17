package com.jumbo.trus.service.weather;

import com.jumbo.trus.dto.weather.MatchWeatherDTO;
import com.jumbo.trus.dto.weather.OpenMeteoResponse;
import com.jumbo.trus.dto.weather.WeatherSnapshot;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.MatchWeatherEntity;
import com.jumbo.trus.entity.weather.WeatherSourceType;
import com.jumbo.trus.mapper.MatchWeatherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive";
    private static final String PROVIDER = "OPEN_METEO";
    private static final BigDecimal PRAGUE_LATITUDE = new BigDecimal("50.0755");
    private static final BigDecimal PRAGUE_LONGITUDE = new BigDecimal("14.4378");
    public static final ZoneId PRAGUE_ZONE = ZoneId.of("Europe/Prague");
    private static final String WEATHER_VARIABLES = String.join(",",
            "temperature_2m",
            "apparent_temperature",
            "relative_humidity_2m",
            "precipitation",
            "rain",
            "snowfall",
            "weather_code",
            "cloud_cover",
            "wind_speed_10m",
            "wind_gusts_10m",
            "wind_direction_10m",
            "surface_pressure"
    );

    private final RestTemplate restTemplate;
    private final MatchWeatherMapper matchWeatherMapper;

    public Optional<MatchWeatherEntity> createWeatherForMatch(MatchEntity match, Date requestedDate) {
        return getWeather(requestedDate).map(snapshot -> toEntity(match, snapshot));
    }

    public MatchWeatherDTO getWeatherDtoWithoutMatch(Date requestedDate) {
        MatchWeatherEntity matchWeatherEntity = createWeatherForMatch(null, requestedDate).orElse(null);
        if (matchWeatherEntity == null) {
            return null;
        }
        return matchWeatherMapper.toDTO(matchWeatherEntity);
    }

    public Optional<WeatherSnapshot> getWeather(Date requestedDate) {
        try {
            if (requestedDate == null) {
                return getCurrentWeather();
            }

            LocalDateTime requestedDateTime = LocalDateTime.ofInstant(requestedDate.toInstant(), PRAGUE_ZONE);
            LocalDate requestedDay = requestedDateTime.toLocalDate();
            LocalDate today = LocalDate.now(PRAGUE_ZONE);

            if (requestedDay.isBefore(today)) {
                return getHourlyWeather(ARCHIVE_URL, requestedDateTime, WeatherSourceType.HISTORICAL, false);
            }

            return getHourlyWeather(FORECAST_URL, requestedDateTime, WeatherSourceType.FORECAST, true);
        } catch (Exception exception) {
            log.warn("Počasí se nepodařilo načíst pro datum {}", requestedDate, exception);
            return Optional.empty();
        }
    }

    private Optional<WeatherSnapshot> getCurrentWeather() {
        String currentVariables = WEATHER_VARIABLES + ",is_day";
        String uri = UriComponentsBuilder.fromUriString(FORECAST_URL)
                .queryParam("latitude", PRAGUE_LATITUDE)
                .queryParam("longitude", PRAGUE_LONGITUDE)
                .queryParam("current", currentVariables)
                .queryParam("timezone", PRAGUE_ZONE.getId())
                .build()
                .toUriString();

        OpenMeteoResponse response = restTemplate.getForObject(uri, OpenMeteoResponse.class);
        if (response == null || response.getCurrent() == null) {
            return Optional.empty();
        }

        OpenMeteoResponse.Current current = response.getCurrent();
        return Optional.of(new WeatherSnapshot(
                current.getTemperature(),
                current.getApparentTemperature(),
                current.getRelativeHumidity(),
                current.getPrecipitation(),
                current.getRain(),
                current.getSnowfall(),
                current.getWeatherCode(),
                current.getCloudCover(),
                current.getWindSpeed(),
                current.getWindGusts(),
                current.getWindDirection(),
                current.getSurfacePressure(),
                toBoolean(current.getDay()),
                current.getTime(),
                defaultCoordinate(response.getLatitude(), PRAGUE_LATITUDE),
                defaultCoordinate(response.getLongitude(), PRAGUE_LONGITUDE),
                PROVIDER,
                WeatherSourceType.CURRENT
        ));
    }

    private Optional<WeatherSnapshot> getHourlyWeather(
            String endpoint,
            LocalDateTime requestedDateTime,
            WeatherSourceType sourceType,
            boolean includeIsDay
    ) {
        LocalDate date = requestedDateTime.toLocalDate();
        String variables = includeIsDay ? WEATHER_VARIABLES + ",is_day" : WEATHER_VARIABLES;

        String uri = UriComponentsBuilder.fromUriString(endpoint)
                .queryParam("latitude", PRAGUE_LATITUDE)
                .queryParam("longitude", PRAGUE_LONGITUDE)
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .queryParam("hourly", variables)
                .queryParam("timezone", PRAGUE_ZONE.getId())
                .build()
                .toUriString();
        OpenMeteoResponse response = restTemplate.getForObject(uri, OpenMeteoResponse.class);
        if (response == null || response.getHourly() == null || response.getHourly().getTime() == null
                || response.getHourly().getTime().isEmpty()) {
            return Optional.empty();
        }

        OpenMeteoResponse.Hourly hourly = response.getHourly();
        int nearestIndex = IntStream.range(0, hourly.getTime().size())
                .boxed()
                .min(Comparator.comparingLong(index -> Math.abs(
                        Duration.between(requestedDateTime, hourly.getTime().get(index)).toMinutes()
                )))
                .orElse(0);

        return Optional.of(new WeatherSnapshot(
                valueAt(hourly.getTemperatures(), nearestIndex),
                valueAt(hourly.getApparentTemperatures(), nearestIndex),
                valueAt(hourly.getRelativeHumidities(), nearestIndex),
                valueAt(hourly.getPrecipitation(), nearestIndex),
                valueAt(hourly.getRain(), nearestIndex),
                valueAt(hourly.getSnowfall(), nearestIndex),
                valueAt(hourly.getWeatherCodes(), nearestIndex),
                valueAt(hourly.getCloudCover(), nearestIndex),
                valueAt(hourly.getWindSpeed(), nearestIndex),
                valueAt(hourly.getWindGusts(), nearestIndex),
                valueAt(hourly.getWindDirection(), nearestIndex),
                valueAt(hourly.getSurfacePressure(), nearestIndex),
                toBoolean(valueAt(hourly.getDay(), nearestIndex)),
                hourly.getTime().get(nearestIndex),
                defaultCoordinate(response.getLatitude(), PRAGUE_LATITUDE),
                defaultCoordinate(response.getLongitude(), PRAGUE_LONGITUDE),
                PROVIDER,
                sourceType
        ));
    }

    private MatchWeatherEntity toEntity(MatchEntity match, WeatherSnapshot snapshot) {
        MatchWeatherEntity entity = new MatchWeatherEntity();
        entity.setMatch(match);
        if (match != null && match.getFootballMatch() != null) {
            entity.setFootballMatchId(match.getFootballMatch().getId());
        }
        entity.setTemperature(snapshot.temperature());
        entity.setApparentTemperature(snapshot.apparentTemperature());
        entity.setRelativeHumidity(snapshot.relativeHumidity());
        entity.setPrecipitation(snapshot.precipitation());
        entity.setRain(snapshot.rain());
        entity.setSnowfall(snapshot.snowfall());
        entity.setWeatherCode(snapshot.weatherCode());
        entity.setCloudCover(snapshot.cloudCover());
        entity.setWindSpeed(snapshot.windSpeed());
        entity.setWindGusts(snapshot.windGusts());
        entity.setWindDirection(snapshot.windDirection());
        entity.setSurfacePressure(snapshot.surfacePressure());
        entity.setDay(snapshot.day());
        entity.setMeasuredAt(snapshot.measuredAt());
        entity.setLatitude(snapshot.latitude());
        entity.setLongitude(snapshot.longitude());
        entity.setProvider(snapshot.provider());
        entity.setSourceType(snapshot.sourceType());
        entity.setCreatedAt(LocalDateTime.now(PRAGUE_ZONE));
        return entity;
    }

    private static <T> T valueAt(List<T> values, int index) {
        return values != null && index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static Boolean toBoolean(Integer value) {
        return value == null ? null : value == 1;
    }

    private static BigDecimal defaultCoordinate(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }
}
