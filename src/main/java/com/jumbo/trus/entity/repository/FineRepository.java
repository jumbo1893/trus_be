package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.FineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FineRepository extends JpaRepository<FineEntity, Long> {

    @Query(value = "SELECT * from fine ORDER BY name ASC LIMIT :limit ", nativeQuery = true)
    List<FineEntity> getAll(@Param("limit") int limit);

    @Query(value = "SELECT * from fine WHERE inactive = false OR inactive IS NULL ORDER BY name ASC LIMIT :limit ", nativeQuery = true)
    List<FineEntity> getAllActive(@Param("limit") int limit);

    @Query(value = "SELECT * from fine WHERE id NOT IN :fineIds AND (inactive = false OR inactive IS NULL) ORDER BY name ASC" , nativeQuery = true)
    List<FineEntity> getAllOtherFines(@Param("fineIds") Collection<Long> fineIds);
}
