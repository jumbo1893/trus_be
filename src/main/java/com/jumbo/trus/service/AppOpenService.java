package com.jumbo.trus.service;

import com.jumbo.trus.entity.AppOpenEntity;
import com.jumbo.trus.repository.AppOpenRepository;
import com.jumbo.trus.service.header.HeaderManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppOpenService {

    private final AppOpenRepository repository;
    private final HeaderManager headers;

    @Transactional
    public void record(UUID eventId, Long userId, String clientDeviceId,
                       String openType, Instant clientOpenedAt, HttpServletRequest request) {
        if (repository.existsByEventId(eventId)) {
            return;
        }
        if (!"START".equals(openType) && !"RESUME".equals(openType)) {
            throw new IllegalArgumentException("Neplatný typ otevření aplikace");
        }

        AppOpenEntity open = new AppOpenEntity();
        open.setEventId(eventId);
        open.setUserId(userId);
        open.setOpenedAt(Instant.now());
        open.setClientOpenedAt(clientOpenedAt);
        open.setOpenType(openType);
        open.setClientDeviceId(limit(clientDeviceId, 100));
        open.setDevice(limit(headers.getDeviceHeader(), 255));
        open.setAppVersion(limit(headers.getAppVersionHeader(), 50));
        open.setIpAddress(limit(headers.getClientIp(), 45));
        open.setUserAgent(limit(request.getHeader("User-Agent"), 255));
        repository.save(open);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replaceAll("[\\r\\n\\t]", " ");
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }
}
