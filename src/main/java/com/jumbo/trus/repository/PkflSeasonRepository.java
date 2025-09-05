package com.jumbo.trus.repository;

import com.jumbo.trus.entity.pkfl.PkflSeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PkflSeasonRepository extends JpaRepository<PkflSeasonEntity, Long> {

    @Query(value = "SELECT * FROM pkfl_season where name like %:season%", nativeQuery = true)
    List<PkflSeasonEntity> getSeasonByNameLike(@Param("season") String season);
    boolean existsByName(String name);

}
