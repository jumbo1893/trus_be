package com.jumbo.trus.repository.membership;

import com.jumbo.trus.entity.membership.MembershipAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MembershipAccountRepository extends JpaRepository<MembershipAccountEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM MembershipAccountEntity account WHERE account.user.id = :userId")
    Optional<MembershipAccountEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}
