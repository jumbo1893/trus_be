package com.jumbo.trus.service.activity.footbar.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.dto.footbar.SessionListResponse;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.mapper.footbar.FootbarRawSessionMapper;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import com.jumbo.trus.service.activity.footbar.FootbarProperties;
import com.jumbo.trus.service.activity.footbar.connect.FootbarConnect;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import jakarta.transaction.Transactional;
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
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootbarSessionProcessor {
    private final com.jumbo.trus.repository.footbar.FootbarAccountRepository footbarAccountRepository;

    private final FootbarSessionRepository footbarSessionRepository;
    private final com.jumbo.trus.service.activity.footbar.FootbarRestTemplate restTemplate;
    private final FootbarConnect footbarConnect;
    private final FootbarProperties footbarProperties;
    private final ObjectMapper mapper;
    private final FootbarRawSessionMapper footbarRawSessionMapper;
    private final MatchService matchService;
    private final OutboxEventService outboxEventService;

    public List<FootbarSessionDTO> fetchSessions(String accessToken) {
        Instant deadline = Instant.now().plusSeconds(20 * 60);
        List<FootbarSessionDTO> allSessions = new ArrayList<>();
        int page = 1;

        while (true) {
            if (Instant.now().isAfter(deadline)) throw new IllegalStateException("Footbar import překročil časový limit.");
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

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void saveSessions(FootbarAccountEntity footbarAccount, AppTeamEntity appTeam) {
        Instant deadline = Instant.now().plusSeconds(20 * 60);
        // Reload in this account's transaction so lazy user/team relations are available.
        footbarAccount = footbarAccountRepository.findById(footbarAccount.getId()).orElseThrow();
        String validAccessToken = footbarConnect.getValidAccessToken(footbarAccount);
        List<FootbarSessionDTO> sessions = fetchSessions(validAccessToken);
        for (FootbarSessionDTO session : sessions) {
            if (Instant.now().isAfter(deadline)) throw new IllegalStateException("Footbar import překročil časový limit.");
            FootbarSessionEntity repoEntity = findByAccountAndSessionId(session, footbarAccount);
            // A session already assigned to another team must not be stolen or
            // unpaired by importing this account's history for another membership.
            if (repoEntity != null && repoEntity.getMatch() != null
                    && repoEntity.getMatch().getAppTeam() != null
                    && !appTeam.getId().equals(repoEntity.getMatch().getAppTeam().getId())) continue;
            FootbarSessionEntity savedSession;
            if(repoEntity == null) {
                FootbarSessionDTO detailedSession = fetchFootbarSessionDetail(session.getFootbarSessionId(), validAccessToken);
                if (detailedSession == null) {
                    throw new IllegalStateException("Footbar neposkytl detail aktivity " + session.getFootbarSessionId());
                }
                FootbarSessionEntity newSession = getFootbarSessionEntity(footbarAccount, detailedSession);
                newSession.setId(null);
                pairSessionWithMatch(newSession, appTeam);
                pairSessionWithPlayer(footbarAccount, newSession, appTeam);
                savedSession = footbarSessionRepository.save(newSession);
            }
            else {
                pairSessionWithMatch(repoEntity, appTeam);
                pairSessionWithPlayer(footbarAccount, repoEntity, appTeam);
                savedSession = footbarSessionRepository.save(repoEntity);
            }
            // A training session need not belong to a team match, and an account
            // need not be paired with a player yet. Keep the imported activity;
            // a later sync can pair it and then request achievement calculation.
            if (savedSession.getMatch() == null || savedSession.getMatch().getSeason() == null
                    || savedSession.getPlayer() == null) {
                log.debug("Footbar session {} saved without complete match/player pairing", savedSession.getId());
                continue;
            }
            if (Objects.equals(savedSession.getNotifiedMatchId(), savedSession.getMatch().getId())
                    && Objects.equals(savedSession.getNotifiedPlayerId(), savedSession.getPlayer().getId())
                    && Objects.equals(savedSession.getNotifiedSeasonId(), savedSession.getMatch().getSeason().getId())) continue;
            outboxEventService.createEventForTeam(OutboxEventType.FOOTBAR_SESSION_SAVED, OutboxAggregateType.FOOTBAR, null,
                    OutboxEventPayloadFactory.footbarUpdated(
                            savedSession.getMatch().getId(),
                            savedSession.getMatch().getSeason().getId(),
                            Set.of(savedSession.getPlayer().getId()),
                            Set.of(savedSession.getId())
                            ), appTeam.getId(), null);
            savedSession.setNotifiedMatchId(savedSession.getMatch().getId());
            savedSession.setNotifiedPlayerId(savedSession.getPlayer().getId());
            savedSession.setNotifiedSeasonId(savedSession.getMatch().getSeason().getId());
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
