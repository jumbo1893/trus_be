package com.jumbo.trus.repository;

import com.jumbo.trus.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    @Query(value = """
            SELECT *
            FROM player
            WHERE fan = :fan
              AND app_team_id = :appTeamId
              AND deleted = false
            ORDER BY name
            """, nativeQuery = true)
    List<PlayerEntity> getAllByFan(
            @Param("fan") boolean fan,
            @Param("appTeamId") Long appTeamId
    );

    @Query(value = """
            SELECT *
            FROM player
            WHERE active = :active
              AND fan = false
              AND app_team_id = :appTeamId
              AND deleted = false
            ORDER BY name
            """, nativeQuery = true)
    List<PlayerEntity> getAllByActive(
            @Param("active") boolean active,
            @Param("appTeamId") Long appTeamId
    );

    @Query(value = """
            SELECT *
            FROM player
            WHERE app_team_id = :appTeamId
              AND deleted = false
            ORDER BY name
            """, nativeQuery = true)
    List<PlayerEntity> getAll(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT *,
                   CASE
                       WHEN TO_DATE(TO_CHAR(birthday, 'MM-DD'), 'MM-DD')
                            >= TO_DATE(TO_CHAR(CURRENT_DATE, 'MM-DD'), 'MM-DD')
                           THEN TO_DATE(TO_CHAR(birthday, 'MM-DD'), 'MM-DD')
                       ELSE TO_DATE(TO_CHAR(birthday, 'MM-DD'), 'MM-DD')
                            + INTERVAL '1 year'
                   END AS next_birthday
            FROM player
            WHERE app_team_id = :appTeamId
              AND deleted = false
            ORDER BY next_birthday ASC
            """, nativeQuery = true)
    List<PlayerEntity> getBirthdayPlayers(@Param("appTeamId") Long appTeamId);

    @Query("""
    SELECT p
    FROM PlayerEntity p
    WHERE p.appTeam.id = :appTeamId
      AND p.deleted = false
    ORDER BY p.name ASC
""")
    List<PlayerEntity> findAllNotDeletedByAppTeam(@Param("appTeamId") Long appTeamId);

}
