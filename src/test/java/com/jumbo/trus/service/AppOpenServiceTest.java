package com.jumbo.trus.service;

import com.jumbo.trus.entity.AppOpenEntity;
import com.jumbo.trus.repository.AppOpenRepository;
import com.jumbo.trus.service.header.HeaderManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOpenServiceTest {

    private final AppOpenRepository repository = mock(AppOpenRepository.class);
    private final HeaderManager headers = mock(HeaderManager.class);
    private final AppOpenService service = new AppOpenService(repository, headers);

    @Test
    void recordsServerUserAndRequestMetadata() {
        UUID eventId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "TrusApp/1.2");
        when(headers.getDeviceHeader()).thenReturn("android 15 - Pixel");
        when(headers.getAppVersionHeader()).thenReturn("1.2");
        when(headers.getClientIp()).thenReturn("192.0.2.10");

        Instant clientOpenedAt = Instant.parse("2026-10-29T10:00:00Z");
        service.record(eventId, 42L, " installation-1 ", "START", clientOpenedAt, request);

        ArgumentCaptor<AppOpenEntity> saved = ArgumentCaptor.forClass(AppOpenEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(42L);
        assertThat(saved.getValue().getEventId()).isEqualTo(eventId);
        assertThat(saved.getValue().getClientDeviceId()).isEqualTo("installation-1");
        assertThat(saved.getValue().getDevice()).isEqualTo("android 15 - Pixel");
        assertThat(saved.getValue().getIpAddress()).isEqualTo("192.0.2.10");
        assertThat(saved.getValue().getOpenType()).isEqualTo("START");
        assertThat(saved.getValue().getOpenedAt()).isNotNull();
        assertThat(saved.getValue().getClientOpenedAt()).isEqualTo(clientOpenedAt);
    }

    @Test
    void duplicateEventIsIgnored() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(true);

        service.record(eventId, 42L, "device", "RESUME", Instant.now(), new MockHttpServletRequest());

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnknownOpenType() {
        assertThrows(IllegalArgumentException.class, () -> service.record(
                UUID.randomUUID(), 42L, "device", "OTHER", Instant.now(), new MockHttpServletRequest()));
    }
}
