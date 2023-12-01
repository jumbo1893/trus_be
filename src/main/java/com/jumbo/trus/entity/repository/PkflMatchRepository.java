package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.pkfl.PkflMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface PkflMatchRepository extends JpaRepository<PkflMatchEntity, Long> {

    @Query(value = "SELECT * FROM pkfl_match WHERE already_played = false AND date > CURRENT_TIMESTAMP ORDER BY DATE ASC", nativeQuery = true)
    List<PkflMatchEntity> getNonPlayedMatchesOrderByDate();

    @Query(value = "SELECT * FROM pkfl_match WHERE season_id IN :#{#seasonIds} AND id IN :#{#ids} ORDER BY DATE DESC", nativeQuery = true)
    List<PkflMatchEntity> findAllByIdsAndSeasonIds(@Param("seasonIds") List<Long> seasonIds, @Param("ids") List<Long> ids);

    @Query(value = "SELECT id FROM pkfl_match", nativeQuery = true)
    List<Long> findAllIds();

    @Query(value = "SELECT id FROM pkfl_match WHERE season_id IN :#{#seasonIds} ORDER BY DATE DESC", nativeQuery = true)
    List<Long> findAllIdsBySeasonIds(@Param("seasonIds") List<Long> seasonIds);

    @Query(value = "SELECT * FROM pkfl_match WHERE already_played = true ORDER BY DATE DESC LIMIT :limit", nativeQuery = true)
    List<PkflMatchEntity> getAlreadyPlayedMatchesOrderByDateDesc(@Param("limit") int limit);

    @Query(value = "SELECT * FROM pkfl_match WHERE already_played = true AND opponent_id = :opponentId ORDER BY DATE DESC", nativeQuery = true)
    List<PkflMatchEntity> findAlreadyPlayedByOpponentId(@Param("opponentId") long opponentId);


    @Query(value = "SELECT * FROM pkfl_match WHERE date > CURRENT_TIMESTAMP ORDER BY DATE ASC LIMIT 1", nativeQuery = true)
    PkflMatchEntity getNextMatch();

    @Query(value = "SELECT * FROM pkfl_match WHERE date < CURRENT_TIMESTAMP ORDER BY DATE DESC LIMIT 1", nativeQuery = true)
    PkflMatchEntity getLastMatch();

    @Query(value = "SELECT * FROM pkfl_match WHERE date BETWEEN :startDate AND :endDate LIMIT 1", nativeQuery = true)
    PkflMatchEntity findByDate(@Param("startDate")Date startDate, @Param("endDate")Date endDate);

    @Query(value = "SELECT * FROM pkfl_match WHERE season_id=:#{#season} ORDER BY DATE DESC", nativeQuery = true)
    List<PkflMatchEntity> getMatchesBySeason(@Param("season") long seasonId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM pkfl_match m WHERE m.opponent.id = :opponentId AND m.season.id = :seasonId AND m.homeMatch = :homeMatch")
    boolean existsByOpponentAndSeasonIdAndHomeMatch(@Param("opponentId") long opponentId, @Param("seasonId") long seasonId, @Param("homeMatch")boolean homeMatch);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM pkfl_match m WHERE m.date < :date")
    boolean existsMatchOlderThenDate(@Param("date")Date date);

    @Query(value = "SELECT * FROM pkfl_match WHERE opponent_id= :#{#opponentId} AND season_id= :#{#seasonId} AND home_match= :#{#homeMatch} LIMIT 1", nativeQuery = true)
    PkflMatchEntity getMatchByOpponentAndSeasonIdAndHomeMatch(@Param("opponentId") long opponentId, @Param("seasonId") long seasonId, @Param("homeMatch") boolean homeMatch);


}
