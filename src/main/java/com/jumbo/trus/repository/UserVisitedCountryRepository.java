package com.jumbo.trus.repository;

import com.jumbo.trus.entity.country.UserVisitedCountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserVisitedCountryRepository
        extends JpaRepository<UserVisitedCountryEntity, Long> {

    boolean existsByUserIdAndCountryCode(
            Long userId,
            String countryCode
    );

    List<UserVisitedCountryEntity>
    findAllByUserIdOrderByFirstVisitedAtAsc(Long userId);

    @Query("""
            SELECT
                r.player.id AS playerId,
                r.player.name AS playerName,
                v.country.code AS code,
                v.country.nameCs AS nameCs,
                v.firstVisitedAt AS firstVisitedAt,
                v.country.continent.code AS continentCode
            FROM UserVisitedCountryEntity v
            JOIN UserTeamRole r ON r.user = v.user
            WHERE r.appTeam.id = :appTeamId
              AND r.player IS NOT NULL
            ORDER BY r.player.name ASC, v.firstVisitedAt ASC
            """)
    List<TeamVisitedCountryProjection> findAllForTeam(@Param("appTeamId") Long appTeamId);

    @Query(value = """
        WITH inserted_country AS (
            INSERT INTO user_visited_country (
                user_id,
                country_code,
                first_visited_at
            )
            VALUES (
                :userId,
                :countryCode,
                :visitedAt
            )
            ON CONFLICT (user_id, country_code) DO NOTHING
            RETURNING
                id,
                user_id,
                country_code,
                first_visited_at
        )
        SELECT
            id,
            user_id,
            country_code,
            first_visited_at
        FROM inserted_country
        """, nativeQuery = true)
    Optional<UserVisitedCountryEntity> insertIfNotExists(
            @Param("userId") Long userId,
            @Param("countryCode") String countryCode,
            @Param("visitedAt") LocalDateTime visitedAt
    );
}
