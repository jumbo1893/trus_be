package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.dto.footbar.FootbarTokenResponse;
import com.jumbo.trus.repository.footbar.*;
import com.jumbo.trus.service.activity.footbar.connect.*;
import com.jumbo.trus.service.activity.footbar.session.FootbarSessionProcessor;
import com.jumbo.trus.service.UpdateService;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FootbarRecoveryTest {
    FootbarAccountRepository accounts;
    FootbarSessionRepository sessions;
    RestTemplate http;
    FootbarConnect connect;
    FootbarAccountEntity account;

    @BeforeEach void setup() {
        accounts = mock(FootbarAccountRepository.class);
        sessions = mock(FootbarSessionRepository.class);
        http = mock(RestTemplate.class);
        FootbarProperties properties = new FootbarProperties();
        properties.setTokenUrl("https://example.test/token");
        properties.setClientId("client");
        connect = new FootbarConnect(accounts, http, properties, null, null, null, sessions);
        account = new FootbarAccountEntity();
        account.setId(1L);
        account.setAccessToken("old-access");
        account.setRefreshToken("old-refresh");
        account.setTokenExpiry(0L);
        when(accounts.findLockedById(1L)).thenReturn(Optional.of(account));
    }

    @Test void invalidGrantPreservesAccountAndHistoryAndStopsRetrying() {
        when(http.postForEntity(anyString(), any(), eq(FootbarTokenResponse.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY,
                        "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertThrows(FootbarReconnectRequiredException.class, () -> connect.getValidAccessToken(account));
        assertNull(account.getRefreshToken());
        assertNull(account.getAccessToken());
        verify(accounts).save(account);
        verify(accounts, never()).deleteByFootbarUserId(any());
        verifyNoInteractions(sessions);
        assertThrows(FootbarReconnectRequiredException.class, () -> connect.getValidAccessToken(account));
        verify(http, times(1)).postForEntity(anyString(), any(), eq(FootbarTokenResponse.class));
    }

    @Test void unauthorizedClientDoesNotEraseCredentials() {
        when(http.postForEntity(anyString(), any(), eq(FootbarTokenResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
        assertThrows(RuntimeException.class, () -> connect.getValidAccessToken(account));
        assertEquals("old-refresh", account.getRefreshToken());
        verify(accounts, never()).save(any());
        verifyNoInteractions(sessions);
    }

    @Test void missingReplacementRefreshTokenKeepsExistingToken() {
        FootbarTokenResponse response = new FootbarTokenResponse();
        response.setAccessToken("new-access");
        response.setExpiresIn(3600L);
        when(http.postForEntity(anyString(), any(), eq(FootbarTokenResponse.class)))
                .thenReturn(ResponseEntity.ok(response));
        assertEquals("new-access", connect.getValidAccessToken(account));
        assertEquals("old-refresh", account.getRefreshToken());
    }

    @Test void staleCallerUsesAlreadyRotatedDatabaseToken() {
        FootbarAccountEntity stale = new FootbarAccountEntity();
        stale.setId(1L);
        account.setTokenExpiry(Instant.now().getEpochSecond() + 3600);
        assertEquals("old-access", connect.getValidAccessToken(stale));
        verifyNoInteractions(http);
    }

    @Test void failedAccountDoesNotPreventOtherAccountImportOrMarkTeamComplete() {
        FootbarSessionProcessor processor = mock(FootbarSessionProcessor.class);
        UpdateService updates = mock(UpdateService.class);
        FootbarService service = new FootbarService(accounts, connect, processor, null, updates, null, null, null, null);
        AppTeamEntity team = new AppTeamEntity();
        FootbarAccountEntity other = new FootbarAccountEntity();
        other.setId(2L);
        when(accounts.findAllAccountsByAppTeam(team)).thenReturn(List.of(account, other));
        doThrow(new FootbarReconnectRequiredException()).when(processor).saveSessions(account, team);
        assertThrows(IllegalStateException.class, () -> service.syncSessions(team));
        verify(processor).saveSessions(other, team);
        verifyNoInteractions(updates);
    }

    @Test void rejectedGrantMakesReconnectButtonAvailable() {
        when(accounts.findByUserId(7L)).thenReturn(Optional.of(account));
        account.setRefreshToken(null);
        FootbarService service = new FootbarService(accounts, connect, null, null, null, null, null, null, null);
        assertFalse(service.getFootbalProfile(7L).getActive());
    }
}
