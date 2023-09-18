package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByMail(String userName);



}

