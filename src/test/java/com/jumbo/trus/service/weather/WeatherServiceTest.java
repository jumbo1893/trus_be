package com.jumbo.trus.service.weather;

import com.jumbo.trus.mapper.MatchWeatherMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import static com.jumbo.trus.service.weather.WeatherService.PRAGUE_ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class WeatherServiceTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final WeatherService weatherService = new WeatherService(
            restTemplate,
            mock(MatchWeatherMapper.class)
    );

    @Test
    void doesNotCallForecastApiOutsideAvailableForecastRange() {
        Date requestedDate = Date.from(
                LocalDate.now(PRAGUE_ZONE)
                        .plusDays(16)
                        .atTime(LocalTime.NOON)
                        .atZone(PRAGUE_ZONE)
                        .toInstant()
        );

        assertThat(weatherService.getWeather(requestedDate)).isEmpty();
        verifyNoInteractions(restTemplate);
    }
}
