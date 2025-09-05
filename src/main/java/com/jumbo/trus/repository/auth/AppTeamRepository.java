package com.jumbo.trus.repository.auth;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppTeamRepository extends JpaRepository<AppTeamEntity, Long> {

    Optional<AppTeamEntity> findByName(String name);

}

