package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.ReceivedFineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReceivedFineRepository extends PagingAndSortingRepository<ReceivedFineEntity, Long>, JpaRepository<ReceivedFineEntity, Long>, JpaSpecificationExecutor<ReceivedFineEntity> {

    @Query(value = "SELECT * from received_fine LIMIT :limit", nativeQuery = true)
    List<ReceivedFineEntity> getAll(@Param("limit") int limit);

}

