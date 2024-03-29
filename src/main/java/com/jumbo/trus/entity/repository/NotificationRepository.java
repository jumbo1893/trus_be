package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends PagingAndSortingRepository<NotificationEntity, Long>, JpaRepository<NotificationEntity, Long>, JpaSpecificationExecutor<NotificationEntity> {

    @Query(value = "SELECT * from notification LIMIT :limit", nativeQuery = true)
    List<NotificationEntity> getAll(@Param("limit") int limit);

}

