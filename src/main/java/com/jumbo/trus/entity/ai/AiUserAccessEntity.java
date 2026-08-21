package com.jumbo.trus.entity.ai;

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
        name = "ai_user_access",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_user_access_user", columnNames = "user_id")
)
@Data
public class AiUserAccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_user_access_seq")
    @SequenceGenerator(name = "ai_user_access_seq", sequenceName = "ai_user_access_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiAccessTier tier = AiAccessTier.STANDARD;

    /** Null znamená neomezený počet dotazů. */
    @Column(name = "daily_limit")
    private Integer dailyLimit = AiAccessTier.STANDARD.getDefaultDailyLimit();

    @Column(nullable = false)
    private boolean enabled = true;

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
