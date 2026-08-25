package com.jumbo.trus.entity.appnotice;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(
        name = "app_notice_receipt",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_app_notice_receipt_notice_user",
                columnNames = {"notice_id", "user_id"}
        )
)
@Data
public class AppNoticeReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_notice_receipt_seq")
    @SequenceGenerator(name = "app_notice_receipt_seq", sequenceName = "app_notice_receipt_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppNoticeEntity notice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @Column(nullable = false)
    private int displayCount = 0;

    @Column(nullable = false)
    private Instant lastDisplayedAt;
}
