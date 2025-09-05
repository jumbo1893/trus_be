package com.jumbo.trus.repository;

import com.jumbo.trus.entity.FineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<FineEntity, Long> {

    @Query(value = "SELECT * from fine ORDER BY name ASC LIMIT :limit ", nativeQuery = true)
    List<FineEntity> getAll(@Param("limit") int limit);

    @Query(value = "SELECT * from fine WHERE app_team_id=:#{#appTeamId} AND inactive = false OR inactive IS NULL ORDER BY name ASC LIMIT :limit ", nativeQuery = true)
    List<FineEntity> getAllActive(@Param("limit") int limit, @Param("appTeamId") Long appTeamId);

    @Query(value = "SELECT * from fine WHERE id NOT IN :fineIds AND (inactive = false OR inactive IS NULL) AND app_team_id=:#{#appTeamId} ORDER BY name ASC" , nativeQuery = true)
    List<FineEntity> getAllOtherFines(@Param("fineIds") Collection<Long> fineIds, @Param("appTeamId") Long appTeamId);

    Optional<FineEntity> findByNameAndAppTeamId(String name, Long appTeamId);
}
