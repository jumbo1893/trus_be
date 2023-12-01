package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.pkfl.PkflOpponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PkflOpponentRepository extends JpaRepository<PkflOpponentEntity, Long> {

    @Query(value = "SELECT * FROM pkfl_opponent where name= :#{#name} LIMIT 1", nativeQuery = true)
    PkflOpponentEntity getOpponentByName(@Param("name") String name);

    boolean existsByName(String name);

}
