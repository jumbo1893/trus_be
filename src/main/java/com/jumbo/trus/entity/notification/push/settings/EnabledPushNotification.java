package com.jumbo.trus.entity.notification.push.settings;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity(name = "enabled_push_notification")
@Table(
        name = "enabled_push_notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_enabled_push_notification_user_type",
                columnNames = {"user_id", "type"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnabledPushNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enabled_push_notification_seq")
    @SequenceGenerator(name = "enabled_push_notification_seq", sequenceName = "enabled_push_notification_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private Boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date modificationTime;

    @PrePersist
    protected void prePersist() {
        if (enabled == null) enabled = true;       // ⬅️ default při insertu
        if (modificationTime == null) modificationTime = new Date();
    }

    @PreUpdate
    protected void preUpdate() {
        modificationTime = new Date();
    }

    public EnabledPushNotification(Boolean enabled) {
        this.enabled = enabled;
    }
}
