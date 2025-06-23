package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.DynamicTextEntity;
import com.jumbo.trus.entity.UpdateEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DynamicTextRepository extends PagingAndSortingRepository<DynamicTextEntity, Long>, JpaRepository<DynamicTextEntity, Long>, JpaSpecificationExecutor<UpdateEntity> {

    @Query("SELECT d.text FROM dynamic_text d WHERE d.name = :name AND d.appTeam = :appTeam ORDER BY d.rank ASC")
    List<String> findTextByNameAndAppTeam(@Param("name") String name, @Param("appTeam") AppTeamEntity appTeam);

    Optional<DynamicTextEntity> findByNameAndRankAndAppTeam(String name, int rank, AppTeamEntity appTeam);

    void deleteByNameAndAppTeamAndRankGreaterThanEqual(String name, AppTeamEntity appTeam, int rank);


}

