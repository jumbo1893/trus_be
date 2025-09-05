package com.jumbo.trus.service.activity;

import com.jumbo.trus.dto.strava.StravaTokenResponse;
import com.jumbo.trus.repository.strava.AthleteRepository;
import com.jumbo.trus.entity.strava.AthleteEntity;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class StravaConnect {

    private final AthleteRepository athleteRepository;
    private final RestTemplate restTemplate;
    private final StravaProperties stravaProperties;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;


    public void exchangeCodeForToken(String code, Long userId) {
        String url = "https://www.strava.com/oauth/token";

        MultiValueMap<String, String> params = getValueMapWithClient();
        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", stravaProperties.getUrl() + stravaProperties.getCallbackEndpoint());

        HttpHeaders headers = getApplicationFormHeaders();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<StravaTokenResponse> response = restTemplate.postForEntity(url, request, StravaTokenResponse.class);
        StravaTokenResponse tokenData = response.getBody();
        assert tokenData != null;
        AthleteEntity athlete = athleteRepository.findByStravaAthleteId(tokenData.getAthlete().getId())
                .orElse(new AthleteEntity());
        athlete.setStravaAthleteId(tokenData.getAthlete().getId());
        athlete.setAccessToken(tokenData.getAccessToken());
        athlete.setRefreshToken(tokenData.getRefreshToken());
        athlete.setTokenExpiry(tokenData.getExpiresAt());
        athlete.setUser(userService.findById(userId));
        athleteRepository.save(athlete);
    }

    public String connectRedirectUrl(Long userId) {
        String token = jwtTokenUtil.generateToken(userId);
        return String.format(
                "https://www.strava.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=activity:read_all&state=%s",
                stravaProperties.getClientId(),
                stravaProperties.getUrl() + stravaProperties.getCallbackEndpoint(),
                token
        );
    }


    public AthleteEntity refreshAccessToken(AthleteEntity athlete) {
        String url = "https://www.strava.com/oauth/token";

        MultiValueMap<String, String> params = getValueMapWithClient();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", athlete.getRefreshToken());

        HttpHeaders headers = getApplicationFormHeaders();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<StravaTokenResponse> response = restTemplate.postForEntity(url, request, StravaTokenResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            StravaTokenResponse tokenData = response.getBody();

            assert tokenData != null;
            athlete.setAccessToken(tokenData.getAccessToken());
            athlete.setRefreshToken(tokenData.getRefreshToken());
            athlete.setTokenExpiry(tokenData.getExpiresAt());
            athleteRepository.save(athlete);

            return athlete;
        } else {
            throw new RuntimeException("Nepodařilo se obnovit Strava access token. Kód: " + response.getStatusCode());
        }
    }

    public String getValidAccessToken(AthleteEntity athlete) {
        long now = Instant.now().getEpochSecond();
        if (athlete.getTokenExpiry() == null || athlete.getTokenExpiry() < now) {
            athlete = refreshAccessToken(athlete);
        }
        return athlete.getAccessToken();
    }

    private MultiValueMap<String, String> getValueMapWithClient() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", stravaProperties.getClientId());
        params.add("client_secret", stravaProperties.getClientSecret());
        return params;
    }

    private HttpHeaders getApplicationFormHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

}