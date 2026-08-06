package com.jumbo.trus.repository;

import com.jumbo.trus.entity.outbox.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {


}
