package com.jumbo.trus.repository.auth;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppTeamRepository extends JpaRepository<AppTeamEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from AppTeamEntity t where t.id = :id")
    Optional<AppTeamEntity> findForReportUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<AppTeamEntity> findByName(String name);

    Optional<AppTeamEntity> findByNameIgnoreCase(String name);
}

