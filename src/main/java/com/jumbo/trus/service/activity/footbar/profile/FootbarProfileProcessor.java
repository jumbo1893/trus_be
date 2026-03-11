package com.jumbo.trus.service.activity.footbar.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.footbar.profile.FootbarProfile;
import com.jumbo.trus.dto.footbar.profile.FootbarProfilePage;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import com.jumbo.trus.service.activity.footbar.FootbarProperties;
import com.jumbo.trus.service.activity.footbar.connect.FootbarConnect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootbarProfileProcessor {

    private final FootbarSessionRepository footbarSessionRepository;
    private final RestTemplate restTemplate;
    private final FootbarConnect footbarConnect;
    private final FootbarProperties footbarProperties;


    public FootbarProfile fetchFootbarProfileDetail(Long footbarUserId, String accessToken) {
        String url = String.format(footbarProperties.returnProfileDetailUrl() + "?user_id=%d", footbarUserId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            ObjectMapper mapper = new ObjectMapper();
            FootbarProfilePage page = mapper.readValue(resp.getBody(), FootbarProfilePage.class);
            FootbarProfile profile = (page.getResults() != null && !page.getResults().isEmpty())
                    ? page.getResults().get(0)
                    : null;

            if (profile != null) {
                profile.setActive(true);
                return profile;
            }
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                footbarConnect.deleteByFootbarUserId(footbarUserId);
                log.debug("Footbar footbarUserId {} smazán z důvodu chybějících autentizací. Nejspíš se odpároval}", footbarUserId);
            }
            else {
                log.error("Footbar profile ERROR: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            }

        } catch (Exception e) {
            log.error("Footbar profile unexpected error: {}", e.getMessage(), e);
        }

        FootbarProfile inactive = new FootbarProfile();
        inactive.setActive(false);
        return inactive;
    }
}