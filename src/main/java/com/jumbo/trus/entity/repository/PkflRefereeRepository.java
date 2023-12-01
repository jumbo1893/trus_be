package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.pkfl.PkflRefereeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PkflRefereeRepository extends JpaRepository<PkflRefereeEntity, Long> {

    @Query(value = "SELECT * FROM pkfl_referee where name= :#{#name} LIMIT 1", nativeQuery = true)
    PkflRefereeEntity getRefereeByName(@Param("name") String name);

    boolean existsByName(String name);

}
