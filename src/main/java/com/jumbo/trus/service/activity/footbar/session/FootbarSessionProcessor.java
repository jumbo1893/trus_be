package com.jumbo.trus.service.activity.footbar.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.dto.footbar.SessionListResponse;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
import com.jumbo.trus.mapper.footbar.FootbarRawSessionMapper;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.activity.footbar.FootbarProperties;
import com.jumbo.trus.service.activity.footbar.connect.FootbarConnect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootbarSessionProcessor {

    private final FootbarSessionRepository footbarSessionRepository;
    private final RestTemplate restTemplate;
    private final FootbarConnect footbarConnect;
    private final FootbarProperties footbarProperties;
    private final ObjectMapper mapper;
    private final FootbarRawSessionMapper footbarRawSessionMapper;
    private final MatchService matchService;

    public List<FootbarSessionDTO> fetchSessions(String accessToken) {
        List<FootbarSessionDTO> allSessions = new ArrayList<>();
        int page = 1;

        while (true) {
            String url = String.format(
                    footbarProperties.returnSessionListUrl()+"?page=%d",
                    page
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<SessionListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    SessionListResponse.class
            );

            SessionListResponse sessionListResponse = response.getBody();
            assert sessionListResponse != null;
            List<FootbarSessionDTO> footbarSessions = sessionListResponse.getResults().stream().map(footbarRawSessionMapper::toDto).toList();
            allSessions.addAll(footbarSessions);
            if (sessionListResponse.getNext() == null) {
                break;
            }
            page++;
        }
        return allSessions;
    }

    public void saveSessions(FootbarAccountEntity footbarAccount, AppTeamEntity appTeam) {
        String validAccessToken = footbarConnect.getValidAccessToken(footbarAccount);
        List<FootbarSessionDTO> sessions = fetchSessions(validAccessToken);
        for (FootbarSessionDTO session : sessions) {
            FootbarSessionEntity repoEntity = findByAccountAndSessionId(session, footbarAccount);
            if(repoEntity == null) {
                FootbarSessionDTO detailedSession = fetchFootbarSessionDetail(session.getFootbarSessionId(), validAccessToken);
                FootbarSessionEntity newSession = getFootbarSessionEntity(footbarAccount, detailedSession);
                newSession.setId(null);
                pairSessionWithMatch(newSession, appTeam);
                pairSessionWithPlayer(footbarAccount, newSession, appTeam);
                footbarSessionRepository.save(newSession);
            }
            else {
                pairSessionWithMatch(repoEntity, appTeam);
                pairSessionWithPlayer(footbarAccount, repoEntity, appTeam);
                footbarSessionRepository.save(repoEntity);
            }
        }
    }

    private void pairSessionWithMatch(FootbarSessionEntity footbarSession, AppTeamEntity appTeam) {
        footbarSession.setMatch(matchService.findMatchByAroundTime(appTeam, footbarSession.getStartDate(), footbarSession.getStopDate()));
    }

    private void pairSessionWithPlayer(FootbarAccountEntity footbarAccount, FootbarSessionEntity footbarSession, AppTeamEntity appTeam) {
        PlayerEntity player = footbarAccount.getUser().getTeamRoles().stream()
                .filter(role -> role.getAppTeam().equals(appTeam))
                .map(UserTeamRole::getPlayer)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        footbarSession.setPlayer(player);
    }

    private boolean existsSessionInRepo(FootbarSessionDTO session, FootbarAccountEntity footbarAccount) {
        return footbarSessionRepository.findByFootbarAccount(footbarAccount).stream()
                .anyMatch(a -> a.getFootbarSessionId() != null &&
                        a.getFootbarSessionId().equals(session.getFootbarSessionId()));
    }

    private FootbarSessionEntity findByAccountAndSessionId(FootbarSessionDTO session, FootbarAccountEntity footbarAccount) {
        return footbarSessionRepository.findByfootbarSessionIdAndFootbarAccount(session.getFootbarSessionId(), footbarAccount).orElse(null);
    }

    public FootbarSessionDTO fetchFootbarSessionDetail(Long footbarSessionId, String accessToken) {
        String url = String.format(
                footbarProperties.returnSessionDetailUrl()+"?id=%d",
                footbarSessionId
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            SessionListResponse page = mapper.readValue(resp.getBody(), SessionListResponse.class);
            return (page.getResults() != null && !page.getResults().isEmpty())
                    ? footbarRawSessionMapper.toDto(page.getResults().get(0))
                    : null;
        } catch (RestClientResponseException e) {
            log.error("Footbar profile ERROR: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Footbar profile unexpected error: {}", e.getMessage(), e);
        }
        return null;
    }

    private FootbarSessionEntity getFootbarSessionEntity(FootbarAccountEntity footbarAccount, FootbarSessionDTO dto) {
        FootbarSessionEntity entity = new FootbarSessionEntity();
        entity.setFootbarAccount(footbarAccount);
        entity.setFootbarSessionId(dto.getFootbarSessionId());
        entity.setStartDate(dto.getStartDate());
        entity.setStopDate(dto.getStopDate());
        entity.setPlayingTime(dto.getPlayingTime());
        entity.setTitle(dto.getTitle());
        entity.setMatchType(dto.getMatchType());
        entity.setPosition(dto.getPosition());
        entity.setScoreStars(dto.getScoreStars());
        entity.setDistance(dto.getDistance());
        entity.setPassCount(dto.getPassCount());
        entity.setShotCount(dto.getShotCount());
        entity.setShotSpeed(dto.getShotSpeed());
        entity.setAvgShotSpeed(dto.getAvgShotSpeed());
        entity.setDribbleCount(dto.getDribbleCount());
        entity.setTimeWithBall(dto.getTimeWithBall());
        entity.setActivity(dto.getActivity());
        entity.setTimeRunning(dto.getTimeRunning());
        entity.setRunCount(dto.getRunCount());
        entity.setSprintCount(dto.getSprintCount());
        entity.setAvgSprintSpeed(dto.getAvgSprintSpeed());
        entity.setSprintSpeed(dto.getSprintSpeed());
        entity.setHsrPlus(dto.getHsrPlus());
        entity.setStopAndGo(dto.getStopAndGo());
        entity.setAcceleration(dto.getAcceleration());
        entity.setSyncedAt(new Date());
        return entity;
    }

    private Date convertStringToDate(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return null;
        }
        Instant instant = Instant.parse(dateTime);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Europe/Prague"));
        return Date.from(zonedDateTime.toInstant());
    }
}