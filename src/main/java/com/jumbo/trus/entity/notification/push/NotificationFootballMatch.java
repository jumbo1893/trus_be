package com.jumbo.trus.entity.notification.push;

import com.jumbo.trus.entity.football.FootballMatchEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "notification_football_match")
@Data
public class NotificationFootballMatch {

    @Id
    @GeneratedValue(generator = "notification_football_match_seq")
    @SequenceGenerator(
            name = "notification_football_match_seq",
            sequenceName = "notification_football_match_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private FootballMatchEntity footballMatch;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean sent = false;
}
