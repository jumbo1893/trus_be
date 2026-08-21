package com.jumbo.trus.repository.ai;

import com.jumbo.trus.entity.ai.AiUserAccessEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiUserAccessRepository extends JpaRepository<AiUserAccessEntity, Long> {

    Optional<AiUserAccessEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT access FROM AiUserAccessEntity access WHERE access.user.id = :userId")
    Optional<AiUserAccessEntity> findByUserIdForUpdate(@Param("userId") Long userId);

    List<AiUserAccessEntity> findAllByOrderByUserNameAsc();
}
