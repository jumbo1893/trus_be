package com.jumbo.trus.repository;

import com.jumbo.trus.dto.receivedfine.IPlayerFineStats;
import com.jumbo.trus.dto.receivedfine.response.stats.projection.IMatchReceivedFineDetail;
import com.jumbo.trus.dto.receivedfine.response.stats.projection.IPlayerReceivedFineDetail;
import com.jumbo.trus.entity.ReceivedFineEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReceivedFineRepository extends PagingAndSortingRepository<ReceivedFineEntity, Long>, JpaRepository<ReceivedFineEntity, Long>, JpaSpecificationExecutor<ReceivedFineEntity> {

    @Query(value = "SELECT * from received_fine LIMIT :limit", nativeQuery = true)
    List<ReceivedFineEntity> getAll(@Param("limit") int limit);

    @Query(value = "SELECT * from received_fine WHERE player_id = :playerId AND fine_id = :fineId AND match_id IN :matchIds", nativeQuery = true)
    List<ReceivedFineEntity> findAllByPlayerIdFineIdAndMatchesId(@Param("playerId") Long playerId, @Param("fineId") Long fineId, @Param("matchIds") List<Long> matchIds);

    @Modifying
    @Query(value = "DELETE from received_fine WHERE player_id=:#{#playerId}", nativeQuery = true)
    void deleteByPlayerId(@Param("playerId") long playerId);

    @Modifying
    @Query(value = "DELETE from received_fine WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByMatchId(@Param("matchId") long matchId);
    @Modifying
    @Query(value = "DELETE from received_fine WHERE fine_id=:#{#fineId}", nativeQuery = true)
    void deleteByFineId(@Param("fineId") long fineId);

    @Modifying
    @Query(value = "UPDATE received_fine SET fine_id=:#{#newFineId} WHERE fine_id=:#{#oldFineId}", nativeQuery = true)
    void updateByFineId(@Param("oldFineId") long oldFineId, @Param("newFineId") long newFineId);

    @Modifying
    @Query(value = "DELETE from received_fine WHERE (fine_id=:#{#goalFineId} OR fine_id=:#{#hattrickFineId}) AND match_id=:#{#matchId}", nativeQuery = true)
    void deleteGoalAndHattrickFinesFromMatch(@Param("matchId") long matchId, @Param("goalFineId") long goalFineId, @Param("hattrickFineId") long hattrickFineId);

    @Query(value = """
                SELECT SUM(r.fine_number) AS total_fines
                FROM received_fine r
                JOIN fine f ON r.fine_id = f.id
                WHERE f.name = :fineName
                AND r.player_id = :playerId
                HAVING SUM(r.fine_number) >= :fineNumber
            """, nativeQuery = true)
    Integer findAtLeastNumberOfFineInHistory(@Param("playerId") Long playerId, @Param("fineName") String fineName, @Param("fineNumber") int fineNumber);

    @Query(value = """
                SELECT r.*
                FROM received_fine r
                JOIN match m ON r.match_id = m.id
                JOIN fine f ON r.fine_id = f.id
                WHERE f.name = :fineName
                AND r.fine_number >= 1
                AND r.player_id = :playerId
                ORDER BY m.date ASC
                LIMIT 1;
            """, nativeQuery = true)
    Optional<ReceivedFineEntity> findFirstOccurrenceOfFine(@Param("playerId") Long playerId, @Param("fineName") String fineName);

    @Query("""
    SELECT
        rf.player.id AS playerId,
        COALESCE(SUM(rf.fine.amount * rf.fineNumber), 0) AS fineAmount,
        COALESCE(SUM(rf.fineNumber), 0) AS fineCount
    FROM received_fine rf
    WHERE rf.appTeam.id = :appTeamId
    GROUP BY rf.player.id
    ORDER BY
        COALESCE(SUM(rf.fine.amount * rf.fineNumber), 0) DESC,
        COALESCE(SUM(rf.fineNumber), 0) DESC,
        rf.player.id ASC
""")
    List<IPlayerFineStats> findTopFineStatsByAppTeam(
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Query("""
    SELECT
        rf.player.id AS playerId,
        COALESCE(SUM(rf.fine.amount * rf.fineNumber), 0) AS fineAmount,
        COALESCE(SUM(rf.fineNumber), 0) AS fineCount
    FROM received_fine rf
    WHERE rf.appTeam.id = :appTeamId
      AND rf.match.season.id = :seasonId
    GROUP BY rf.player.id
    ORDER BY
        COALESCE(SUM(rf.fine.amount * rf.fineNumber), 0) DESC,
        COALESCE(SUM(rf.fineNumber), 0) DESC,
        rf.player.id ASC
""")
    List<IPlayerFineStats> findTopFineStatsByAppTeamAndSeason(
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId,
            Pageable pageable
    );

    @Query("""
    SELECT
        rf.player.id AS playerId,
        rf.player.name AS playerName,
        rf.fine.id AS fineId,
        rf.fine.name AS fineName,
        rf.fine.amount AS fineAmount,
        SUM(rf.fineNumber) AS fineCount,
        SUM(rf.fine.amount * rf.fineNumber) AS totalAmount
    FROM received_fine rf
    WHERE rf.appTeam.id = :appTeamId
      AND rf.match.id = :matchId
      AND rf.fineNumber > 0
    GROUP BY
        rf.player.id,
        rf.player.name,
        rf.fine.id,
        rf.fine.name,
        rf.fine.amount
    ORDER BY
        rf.player.name ASC,
        rf.fine.name ASC
    """)
    List<IMatchReceivedFineDetail> findMatchFineDetail(
            @Param("matchId") Long matchId,
            @Param("appTeamId") Long appTeamId
    );

    @Query("""
    SELECT
        rf.match.id AS matchId,
        rf.match.name AS matchName,
        rf.match.date AS matchDate,
        rf.match.season.id AS seasonId,
        rf.fine.id AS fineId,
        rf.fine.name AS fineName,
        rf.fine.amount AS fineAmount,
        SUM(rf.fineNumber) AS fineCount,
        SUM(rf.fine.amount * rf.fineNumber) AS totalAmount
    FROM received_fine rf
    WHERE rf.appTeam.id = :appTeamId
      AND rf.player.id = :playerId
      AND rf.fineNumber > 0
      AND (
          :seasonId IS NULL
          OR :seasonId = :allSeasonId
          OR rf.match.season.id = :seasonId
      )
    GROUP BY
        rf.match.id,
        rf.match.name,
        rf.match.date,
        rf.match.season.id,
        rf.fine.id,
        rf.fine.name,
        rf.fine.amount
    ORDER BY
        rf.match.date DESC,
        rf.fine.name ASC
    """)
    List<IPlayerReceivedFineDetail> findPlayerFineDetail(
            @Param("playerId") Long playerId,
            @Param("seasonId") Long seasonId,
            @Param("allSeasonId") Long allSeasonId,
            @Param("appTeamId") Long appTeamId
    );

    @Modifying
    @Query(value = """
        DELETE FROM received_fine rf
        USING fine f
        WHERE rf.fine_id = f.id
          AND rf.match_id = :matchId
          AND rf.app_team_id = :appTeamId
          AND f.name IN (
              'Prohra o 5 a více (pro hrající)',
              'Prohra (pro hrající)',
              'Neúčast při 7. a méně lidech',
              'Neúčast v zápase (výhra)',
              'Neúčast v zápase (remíza)',
              'Neúčast v zápase (prohra)'
          )
        """, nativeQuery = true)
    void  deleteAutomaticResultFinesFromMatch(
            @Param("matchId") Long matchId,
            @Param("appTeamId") Long appTeamId
    );
}

