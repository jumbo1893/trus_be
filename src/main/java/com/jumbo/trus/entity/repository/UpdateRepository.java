package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.NotificationEntity;
import com.jumbo.trus.entity.UpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface UpdateRepository extends PagingAndSortingRepository<UpdateEntity, Long>, JpaRepository<UpdateEntity, Long>, JpaSpecificationExecutor<UpdateEntity> {

    @Query(value = "SELECT * from update WHERE name=:#{#name}", nativeQuery = true)
    UpdateEntity getDateByName(@Param("name") String name);

}

