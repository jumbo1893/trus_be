package com.jumbo.trus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_history", indexes = {
        @Index(name = "idx_login_history_user_time", columnList = "user_id, opened_at"),
        @Index(name = "idx_login_history_device_time", columnList = "client_device_id, opened_at")
})
@Getter
@Setter
public class AppOpenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "client_opened_at", updatable = false)
    private Instant clientOpenedAt;

    @Column(name = "open_type", nullable = false, length = 20, updatable = false)
    private String openType;

    @Column(name = "client_device_id", length = 100, updatable = false)
    private String clientDeviceId;

    @Column(name = "device", length = 255, updatable = false)
    private String device;

    @Column(name = "app_version", length = 50, updatable = false)
    private String appVersion;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 255, updatable = false)
    private String userAgent;
}
