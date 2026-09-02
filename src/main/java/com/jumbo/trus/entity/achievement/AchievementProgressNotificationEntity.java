package com.jumbo.trus.entity.achievement;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "achievement_progress_notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_achievement_progress_notification",
                columnNames = {"player_achievement_id", "context_key", "proximity_threshold"}
        )
)
@Data
@NoArgsConstructor
public class AchievementProgressNotificationEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "achievement_progress_notification_seq"
    )
    @SequenceGenerator(
            name = "achievement_progress_notification_seq",
            sequenceName = "achievement_progress_notification_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_achievement_id", nullable = false)
    private PlayerAchievementEntity playerAchievement;

    @Column(name = "context_key", nullable = false, length = 80)
    private String contextKey;

    @Column(name = "proximity_threshold", nullable = false)
    private Long proximityThreshold;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AchievementProgressNotificationEntity(
            PlayerAchievementEntity playerAchievement,
            String contextKey,
            long proximityThreshold
    ) {
        this.playerAchievement = playerAchievement;
        this.contextKey = contextKey;
        this.proximityThreshold = proximityThreshold;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
