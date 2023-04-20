package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonRepository extends JpaRepository<SeasonEntity, Long> {

    @Query(value = "SELECT * from season LIMIT :limit", nativeQuery = true)
    List<SeasonEntity> getAll(@Param("limit") int limit);
}
