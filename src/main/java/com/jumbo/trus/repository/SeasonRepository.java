package com.jumbo.trus.repository;

import com.jumbo.trus.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<SeasonEntity, Long> {

    Optional<SeasonEntity> findByAppTeamIdAndAutomaticKey(Long appTeamId, String automaticKey);

    Optional<SeasonEntity> findFirstByAppTeamIdAndName(Long appTeamId, String name);

    @Query(value = """
            SELECT * FROM season
            WHERE to_date < :tomorrow
              AND app_team_id IS NOT NULL
              AND (achievement_event_for_end IS NULL OR achievement_event_for_end <> to_date)
            ORDER BY to_date, id
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SeasonEntity> findDueForAchievements(@Param("tomorrow") java.util.Date tomorrow);

    @Query("""
            SELECT s
            FROM season s
            WHERE s.id = :seasonId
              AND s.appTeam.id = :appTeamId
            """)
    Optional<SeasonEntity> findByIdAndAppTeamId(
            @Param("seasonId") Long seasonId,
            @Param("appTeamId") Long appTeamId
    );

    @Query(value = "SELECT * from season WHERE app_team_id=:#{#appTeamId} LIMIT :limit", nativeQuery = true)
    List<SeasonEntity> getAll(@Param("limit") int limit, @Param("appTeamId") Long appTeamId);

    @Query(value = "SELECT * from season where editable = true AND app_team_id=:#{#appTeamId} LIMIT :limit", nativeQuery = true)
    List<SeasonEntity> getAllWithoutNonEditable(@Param("limit") int limit, @Param("appTeamId") Long appTeamId);
}
