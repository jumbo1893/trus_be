package com.jumbo.trus.entity.membership;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(
        name = "achievement_membership_credit",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_achievement_membership_credit_source",
                columnNames = {"user_id", "player_achievement_id"}
        )
)
@Data
public class AchievementMembershipCreditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "achievement_membership_credit_seq")
    @SequenceGenerator(name = "achievement_membership_credit_seq", sequenceName = "achievement_membership_credit_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @Column(name = "player_achievement_id", nullable = false)
    private Long playerAchievementId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
