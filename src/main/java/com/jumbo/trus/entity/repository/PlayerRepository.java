package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    @Query(value = "SELECT * from player WHERE fan=:#{#fan}", nativeQuery = true)
    List<PlayerEntity> getAllByFan(@Param("fan") boolean fan);

    @Query(value = "SELECT * from player LIMIT :limit", nativeQuery = true)
    List<PlayerEntity> getAll(@Param("limit") int limit);
}
