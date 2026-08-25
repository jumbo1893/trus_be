package com.jumbo.trus.entity.appnotice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "app_notice_action")
@Data
public class AppNoticeActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_notice_action_seq")
    @SequenceGenerator(name = "app_notice_action_seq", sequenceName = "app_notice_action_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppNoticeEntity notice;

    @Column(nullable = false, length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private AppNoticeActionType type = AppNoticeActionType.CLOSE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppNoticeActionStyle style = AppNoticeActionStyle.PRIMARY;

    @Column(name = "action_value", length = 2_000)
    private String value;

    @Column(nullable = false)
    private int displayOrder = 0;
}
