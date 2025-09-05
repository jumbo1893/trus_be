package com.jumbo.trus.repository;

import com.jumbo.trus.entity.ReceivedFineEntity;
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

}

