package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonRepository extends JpaRepository<SeasonEntity, Long> {

    @Query(value = "SELECT * from season LIMIT :limit", nativeQuery = true)
    List<SeasonEntity> getAll(@Param("limit") int limit);

    @Query(value = "SELECT * from season where editable = true LIMIT :limit", nativeQuery = true)
    List<SeasonEntity> getAllWithoutNonEditable(@Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE season_matches SET season_id=-2 WHERE season_id=:#{#seasonId}", nativeQuery = true)
    void updateSeasonId(@Param("seasonId") long seasonId);
}
