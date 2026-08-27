package com.jumbo.trus.repository.membership;

import com.jumbo.trus.entity.membership.AchievementMembershipCreditEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AchievementMembershipCreditRepository extends JpaRepository<AchievementMembershipCreditEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT credit
            FROM AchievementMembershipCreditEntity credit
            WHERE credit.user.id = :userId
              AND credit.playerAchievementId = :playerAchievementId
            """)
    Optional<AchievementMembershipCreditEntity> findForUpdate(
            @Param("userId") Long userId,
            @Param("playerAchievementId") Long playerAchievementId
    );
}
