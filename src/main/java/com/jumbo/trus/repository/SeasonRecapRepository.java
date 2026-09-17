package com.jumbo.trus.repository;

import com.jumbo.trus.entity.SeasonRecapEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface SeasonRecapRepository extends JpaRepository<SeasonRecapEntity, Long> {
    boolean existsBySeasonIdAndUserId(Long seasonId, Long userId);
    List<SeasonRecapEntity> findByUserIdAndSeasonAppTeamIdOrderBySeasonToDateDescIdDesc(Long userId, Long teamId);
    Optional<SeasonRecapEntity> findByIdAndUserId(Long id, Long userId);
    @Query(value = "SELECT pg_try_advisory_xact_lock(903150902)", nativeQuery = true)
    boolean tryPublicationLock();
    @Modifying @Query("delete from SeasonRecapEntity r where r.user.id = :userId")
    void deleteForUser(@Param("userId") Long userId);
}
