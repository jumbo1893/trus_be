package com.jumbo.trus.entity.notification.push;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity(name = "sent_push_notification")
@Data
@NoArgsConstructor
public class SentPushNotification {

    @Id
    @GeneratedValue(generator = "sent_push_notification_seq")
    @SequenceGenerator(name = "sent_push_notification_seq", sequenceName = "sent_push_notification_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_token_id")
    private DeviceToken deviceToken;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date sentTime;

    private String status;

    public SentPushNotification(DeviceToken deviceToken, String title, String body, Date sentTime, String status) {
        this.deviceToken = deviceToken;
        this.title = title;
        this.body = body;
        this.sentTime = sentTime;
        this.status = status;
    }
}
