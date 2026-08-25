package com.jumbo.trus.entity.appnotice;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_notice")
@Data
public class AppNoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_notice_seq")
    @SequenceGenerator(name = "app_notice_seq", sequenceName = "app_notice_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10_000)
    private String message;

    @Column(name = "min_app_version", length = 50)
    private String minAppVersion;

    @Column(name = "max_app_version", length = 50)
    private String maxAppVersion;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean repeatable = false;

    @Column(nullable = false)
    private boolean dismissible = true;

    @Column(nullable = false)
    private int priority = 0;

    private Instant validFrom;

    private Instant validUntil;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AppNoticeActionEntity> actions = new ArrayList<>();
}
