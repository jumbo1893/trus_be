package com.jumbo.trus.repository;

import com.jumbo.trus.entity.AppOpenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AppOpenRepository extends JpaRepository<AppOpenEntity, Long> {
    boolean existsByEventId(UUID eventId);

    @Query("select a.userId as userId, max(a.openedAt) as lastOpenedAt " +
            "from AppOpenEntity a where a.userId in :userIds group by a.userId")
    List<LastOpen> findLastOpenByUserIds(@Param("userIds") Collection<Long> userIds);

    interface LastOpen {
        Long getUserId();
        Instant getLastOpenedAt();
    }
}
