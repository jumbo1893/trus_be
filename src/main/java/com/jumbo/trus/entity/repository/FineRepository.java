package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FineRepository extends JpaRepository<FineEntity, Long> {

    @Query(value = "SELECT * from fine LIMIT :limit", nativeQuery = true)
    List<FineEntity> getAll(@Param("limit") int limit);
}
