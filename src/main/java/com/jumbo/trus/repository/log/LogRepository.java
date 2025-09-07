package com.jumbo.trus.repository.log;

import com.jumbo.trus.entity.log.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogEntity, Long> {

}

