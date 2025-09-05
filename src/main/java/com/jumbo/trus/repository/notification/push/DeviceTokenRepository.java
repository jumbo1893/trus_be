package com.jumbo.trus.repository.notification.push;

import com.jumbo.trus.entity.notification.NotificationEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends PagingAndSortingRepository<DeviceToken, Long>, JpaRepository<DeviceToken, Long>, JpaSpecificationExecutor<NotificationEntity> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUser_IdIn(List<Long> userIds);

    @Query("""
            SELECT DISTINCT dt
            FROM DeviceToken dt
            JOIN dt.user u
            JOIN u.teamRoles utr
            WHERE utr.player.id = :playerId
            """)
    List<DeviceToken> findDeviceTokensByPlayerId(@Param("playerId") Long playerId);

}

