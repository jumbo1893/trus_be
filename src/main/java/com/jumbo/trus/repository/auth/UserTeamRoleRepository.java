package com.jumbo.trus.repository.auth;

import com.jumbo.trus.entity.auth.UserTeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTeamRoleRepository extends JpaRepository<UserTeamRole, Long> {

    Optional<UserTeamRole> findByUserIdAndAppTeamId(Long userId, Long appTeamId);

    List<UserTeamRole> findAllByAppTeamId(Long appTeamId);

    List<UserTeamRole> findAllByPlayerId(Long playerId);

    @Query("""
            SELECT r
            FROM UserTeamRole r
            JOIN FETCH r.user
            WHERE r.appTeam.id = :appTeamId
              AND r.player.id = :playerId
              AND r.user.id <> :userId
            ORDER BY r.id
            """)
    List<UserTeamRole> findPlayerAssignmentsOfOtherUsers(
            @Param("appTeamId") Long appTeamId,
            @Param("playerId") Long playerId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT DISTINCT r.player.id
            FROM UserTeamRole r
            WHERE r.appTeam.id = :appTeamId
              AND r.player IS NOT NULL
              AND EXISTS (
                  SELECT c.id
                  FROM StepConsentEntity c
                  WHERE c.user = r.user
                    AND c.appTeam = r.appTeam
                    AND c.enabled = true
              )
            """)
    List<Long> findConsentingPlayerIdsByAppTeamId(@Param("appTeamId") Long appTeamId);

}

