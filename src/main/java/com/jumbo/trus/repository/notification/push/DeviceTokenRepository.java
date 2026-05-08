package com.jumbo.trus.repository.notification.push;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends PagingAndSortingRepository<DeviceToken, Long>, JpaRepository<DeviceToken, Long>, JpaSpecificationExecutor<DeviceToken> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUser_IdIn(List<Long> userIds);

    @Query("""
            SELECT DISTINCT dt
            FROM DeviceToken dt
            JOIN dt.user u
            JOIN u.teamRoles utr
            WHERE utr.player.id = :playerId
            AND dt.status = :status
            """)
    List<DeviceToken> findDeviceTokensByPlayerId(@Param("playerId") Long playerId, @Param("status") String status);

    @Query("""
    SELECT u
    FROM UserEntity u
    WHERE EXISTS (
        SELECT 1
        FROM DeviceToken dt
        WHERE dt.user = u
    )
    AND EXISTS (
        SELECT 1
        FROM UserEntity u2
        JOIN u2.teamRoles utr
        WHERE u2 = u
          AND utr.appTeam.id = :appTeamId
          AND utr.role = 'ADMIN'
    )
    ORDER BY u.name ASC, u.id ASC
""")
    List<UserEntity> findAdminUsersByAppTeamOrdered(@Param("appTeamId") Long appTeamId);

    @Query("""
    SELECT DISTINCT dt
    FROM DeviceToken dt
    JOIN dt.user u
    JOIN u.teamRoles utr
    WHERE utr.appTeam.id = :appTeamId
      AND dt.status = :status
""")
    List<DeviceToken> findDeviceTokensByAppTeamIdAndStatus(
            @Param("appTeamId") Long appTeamId,
            @Param("status") String status
    );

    List<DeviceToken> findByUser_IdAndClientDeviceIdAndStatus(
            Long userId,
            String clientDeviceId,
            String status
    );

    Optional<DeviceToken> findByUser_IdAndClientDeviceIdAndToken(
            Long userId,
            String clientDeviceId,
            String token
    );
}

