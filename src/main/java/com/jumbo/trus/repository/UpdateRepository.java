package com.jumbo.trus.repository;

import com.jumbo.trus.entity.UpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface UpdateRepository extends PagingAndSortingRepository<UpdateEntity, Long>, JpaRepository<UpdateEntity, Long>, JpaSpecificationExecutor<UpdateEntity> {

    @Query(value = "SELECT * from update WHERE name=:#{#name}", nativeQuery = true)
    UpdateEntity getUpdateByName(@Param("name") String name);

}

