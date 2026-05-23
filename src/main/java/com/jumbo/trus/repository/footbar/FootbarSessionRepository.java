package com.jumbo.trus.repository.footbar;

import com.jumbo.trus.dto.footbar.IPlayerRunningStats;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FootbarSessionRepository extends JpaRepository<FootbarSessionEntity, Long> {
    List<FootbarSessionEntity> findByFootbarAccount(FootbarAccountEntity footbarAccount);

    Optional<FootbarSessionEntity> findByfootbarSessionIdAndFootbarAccount(Long footbarSessionId, FootbarAccountEntity footbarAccount);


    @Query("""
                SELECT a FROM FootbarSessionEntity a
                WHERE a.footbarAccount.user IN (
                    SELECT r.user FROM UserTeamRole r WHERE r.appTeam = :appTeam
                )
                AND a.match = :match
            """)
    List<FootbarSessionEntity> findSessionsByAppTeamAndMatch(
            @Param("appTeam") AppTeamEntity appTeam,
            @Param("match") MatchEntity match
    );

    void deleteByFootbarAccountId(Long footbarAccountId);

    @Query(value = """
               SELECT COALESCE(SUM(a.distance), 0)
             FROM FootbarSessionEntity a
             WHERE a.match.season.id = :seasonId
             AND a.player.id = :playerId
            """)
    Double findDistanceByPlayerIdAndSeasonIdAndAppTeam(
            @Param("seasonId") Long seasonId,
            @Param("playerId") Long playerId
    );

    @Query("""
    SELECT
        fs.player.id AS playerId,
        COALESCE(AVG(fs.distance), 0.0) / 1000.0 AS averageDistance,
        COALESCE(SUM(fs.distance), 0.0) / 1000.0 AS totalDistance
    FROM FootbarSessionEntity fs
    WHERE fs.match.appTeam.id = :appTeamId
      AND fs.distance IS NOT NULL
    GROUP BY fs.player.id
    ORDER BY
        AVG(fs.distance) DESC,
        SUM(fs.distance) DESC,
        fs.player.id ASC
""")
    List<IPlayerRunningStats> findTopRunningStatsByAppTeam(
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Query("""
    SELECT
        fs.player.id AS playerId,
        COALESCE(AVG(fs.distance), 0.0) / 1000.0 AS averageDistance,
        COALESCE(SUM(fs.distance), 0.0) / 1000.0 AS totalDistance
    FROM FootbarSessionEntity fs
    WHERE fs.match.appTeam.id = :appTeamId
      AND fs.match.season.id = :seasonId
      AND fs.distance IS NOT NULL
    GROUP BY fs.player.id
    ORDER BY
        AVG(fs.distance) DESC,
        SUM(fs.distance) DESC,
        fs.player.id ASC
""")
    List<IPlayerRunningStats> findTopRunningStatsByAppTeamAndSeason(
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId,
            Pageable pageable
    );
}
