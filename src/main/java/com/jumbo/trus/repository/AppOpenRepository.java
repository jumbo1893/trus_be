package com.jumbo.trus.repository;

import com.jumbo.trus.entity.AppOpenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppOpenRepository extends JpaRepository<AppOpenEntity, Long> {
    boolean existsByEventId(UUID eventId);
}
