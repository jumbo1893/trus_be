package com.jumbo.trus.service.weather;

import com.jumbo.trus.mapper.MatchWeatherMapper;
import com.jumbo.trus.dto.weather.OpenMeteoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import static com.jumbo.trus.service.weather.WeatherService.PRAGUE_ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

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

    @Test
    void forecastConnectionFailureDoesNotPreventHomeFromLoading() {
        Date requestedDate = Date.from(LocalDate.now(PRAGUE_ZONE).plusDays(1)
                .atTime(LocalTime.NOON).atZone(PRAGUE_ZONE).toInstant());
        when(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class)))
                .thenThrow(new ResourceAccessException("Connection reset"));

        assertThat(weatherService.getWeatherDtoWithoutMatch(requestedDate)).isNull();
    }
}
