package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.pkfl.PkflStadiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PkflStadiumRepository extends JpaRepository<PkflStadiumEntity, Long> {

    @Query(value = "SELECT * FROM pkfl_stadium where name= :#{#name} LIMIT 1", nativeQuery = true)
    PkflStadiumEntity getStadiumByName(@Param("name") String name);

    boolean existsByName(String name);

}
