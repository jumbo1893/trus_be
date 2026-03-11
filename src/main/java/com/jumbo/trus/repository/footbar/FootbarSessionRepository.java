package com.jumbo.trus.repository.footbar;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
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
}
