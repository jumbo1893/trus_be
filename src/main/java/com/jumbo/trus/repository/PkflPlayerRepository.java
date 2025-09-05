package com.jumbo.trus.repository;

import com.jumbo.trus.entity.pkfl.PkflPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PkflPlayerRepository extends JpaRepository<PkflPlayerEntity, Long> {

    @Query(value = "SELECT * FROM pkfl_player where name= :#{#name} LIMIT 1", nativeQuery = true)
    PkflPlayerEntity getPlayerByName(@Param("name") String name);

    boolean existsByName(String name);

}
