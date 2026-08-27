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
        name = "membership_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_membership_account_user", columnNames = "user_id")
)
@Data
public class MembershipAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "membership_account_seq")
    @SequenceGenerator(name = "membership_account_seq", sequenceName = "membership_account_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @Column(name = "ultra_millis_remaining", nullable = false)
    private long ultraMillisRemaining;

    @Column(name = "premium_millis_remaining", nullable = false)
    private long premiumMillisRemaining;

    /** Časově omezené členství udělené administrátorem. */
    @Column(name = "granted_ultra_millis_remaining", nullable = false, columnDefinition = "bigint default 0")
    private long grantedUltraMillisRemaining;

    /** Časově omezené členství udělené administrátorem. */
    @Column(name = "granted_premium_millis_remaining", nullable = false, columnDefinition = "bigint default 0")
    private long grantedPremiumMillisRemaining;

    /** Neomezené členství udělené administrátorem. */
    @Enumerated(EnumType.STRING)
    @Column(name = "unlimited_tier", nullable = false, length = 20, columnDefinition = "varchar(20) default 'STANDARD'")
    private MembershipTier unlimitedTier = MembershipTier.STANDARD;

    @Column(name = "balance_updated_at", nullable = false)
    private Instant balanceUpdatedAt;

    /** Poslední dosažená desítka nápojů, za kterou už bylo členství vyhodnoceno. */
    @Column(name = "last_drink_milestone", nullable = false)
    private long lastDrinkMilestone;

    @Column(name = "counted_drinks", nullable = false)
    private long countedDrinks;

    @Column(name = "drink_counting_started_at", nullable = false, updatable = false)
    private Instant drinkCountingStartedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (balanceUpdatedAt == null) {
            balanceUpdatedAt = now;
        }
        if (drinkCountingStartedAt == null) {
            drinkCountingStartedAt = now;
        }
        if (unlimitedTier == null) {
            unlimitedTier = MembershipTier.STANDARD;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
