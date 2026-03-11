package com.jumbo.trus.service.activity.footbar.connect;

import com.jumbo.trus.dto.footbar.FootbarTokenResponse;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.repository.footbar.FootbarAccountRepository;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import com.jumbo.trus.service.OAuthStateService;
import com.jumbo.trus.service.activity.footbar.FootbarProperties;
import com.jumbo.trus.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootbarConnect {

    private final FootbarAccountRepository footbarAccountRepository;
    private final RestTemplate restTemplate;
    private final FootbarProperties footbarProperties;
    private final UserService userService;
    private final PKCEGenerator pkceGenerator;
    private final OAuthStateService oAuthStateService;
    private final FootbarSessionRepository footbarSessionRepository;
    public final static String SYSTEM = "footbar";

    @Transactional
    public void exchangeCodeForToken(String code) {
            String tokenUrl = footbarProperties.getTokenUrl();
            MultiValueMap<String, String> params = getValueMapWithClient();
            setParamsForAccessToken(params);
            params.add("code", code);
            params.add("code_verifier", oAuthStateService.getCodeVerifier(SYSTEM));

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, getApplicationFormHeaders());
            ResponseEntity<FootbarTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, FootbarTokenResponse.class);

            FootbarTokenResponse tokenData = response.getBody();
            assert tokenData != null;

            FootbarAccountEntity footbarAccount = footbarAccountRepository.findByFootbarUserId(tokenData.getUser().getId())
                    .orElse(new FootbarAccountEntity());
            footbarAccount.setFootbarUserId(tokenData.getUser().getId());
            footbarAccount.setAccessToken(tokenData.getAccessToken());
            footbarAccount.setRefreshToken(tokenData.getRefreshToken());
            footbarAccount.setTokenExpiry(Instant.now().getEpochSecond()+tokenData.getExpiresIn());
            footbarAccount.setUser(userService.getCurrentUserEntity());
            footbarAccount.setLinkedAt(Instant.now());
            footbarAccount.setLastSyncAt(Instant.now());
            footbarAccountRepository.save(footbarAccount);
            oAuthStateService.deleteCodeVerifier(SYSTEM);
    }

    @Transactional
    public void deleteByFootbarUserId(Long footbalUserId) {
        List<FootbarAccountEntity> accountEntityList = footbarAccountRepository.findAllByFootbarUserId(footbalUserId);
        for (FootbarAccountEntity footbarAccount : accountEntityList) {
            footbarSessionRepository.deleteByFootbarAccountId(footbarAccount.getId());
        }
        footbarAccountRepository.deleteByFootbarUserId(footbalUserId);
    }

    private void setParamsForAccessToken(MultiValueMap<String, String> params) {
        params.add("client_secret", footbarProperties.getClientSecret());
        params.add("grant_type", "authorization_code");
        params.add("scope", "read");
    }

    private HttpHeaders getApplicationFormHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-cache");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    public String connectRedirectUrl() {
        Map<String, String> pkcePair = pkceGenerator.generatePkcePair();
        String codeChallenge = pkcePair.get("challenge");
        String codeVerifier = pkcePair.get("verifier");
        oAuthStateService.addCodeVerifier(codeVerifier, SYSTEM);
        return String.format(
                "https://api.footbar.com/oauth/authorize?" +
                        "response_type=code&" +
                        "code_challenge=%s&" +
                        "code_challenge_method=S256&" +
                        "client_id=%s&" +
                        "redirect_uri=%s",
                codeChallenge,
                footbarProperties.getClientId(),
                footbarProperties.getServerUrl() + footbarProperties.getCallbackEndpoint()
        );
    }


    public FootbarAccountEntity refreshAccessToken(FootbarAccountEntity footbarAccountEntity) {
        try {
            String tokenUrl = footbarProperties.getTokenUrl();

            MultiValueMap<String, String> params = getValueMapWithClient();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", footbarAccountEntity.getRefreshToken());
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, getApplicationFormHeaders());

            ResponseEntity<FootbarTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, FootbarTokenResponse.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                FootbarTokenResponse tokenData = response.getBody();

                assert tokenData != null;
                footbarAccountEntity.setAccessToken(tokenData.getAccessToken());
                footbarAccountEntity.setRefreshToken(tokenData.getRefreshToken());
                footbarAccountEntity.setTokenExpiry(Instant.now().getEpochSecond() + tokenData.getExpiresIn());
                footbarAccountEntity.setLastSyncAt(Instant.now());
                footbarAccountRepository.save(footbarAccountEntity);

                return footbarAccountEntity;
            }
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                deleteByFootbarUserId(footbarAccountEntity.getFootbarUserId());
                log.debug("Footbar footbarUserId {} smazán z důvodu chybějících autentizací. Nejspíš se odpároval", footbarAccountEntity.getFootbarUserId());

            }
            log.debug("chyba {}", body);
            throw new RuntimeException("Nepodařilo se obnovit Strava access token. Kód: " + e.getStatusCode());

        }
        return null;
    }

    public String getValidAccessToken(FootbarAccountEntity footbarAccountEntity) {
        long now = Instant.now().getEpochSecond();
        if (footbarAccountEntity.getTokenExpiry() == null || footbarAccountEntity.getTokenExpiry() < now) {
            footbarAccountEntity = refreshAccessToken(footbarAccountEntity);
        }
        return footbarAccountEntity.getAccessToken();
    }

    private MultiValueMap<String, String> getValueMapWithClient() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", footbarProperties.getClientId());
        return params;
    }
}