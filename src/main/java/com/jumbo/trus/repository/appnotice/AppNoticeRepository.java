package com.jumbo.trus.repository.appnotice;

import com.jumbo.trus.entity.appnotice.AppNoticeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNoticeRepository extends JpaRepository<AppNoticeEntity, Long> {

    @EntityGraph(attributePaths = "actions")
    List<AppNoticeEntity> findAllByActiveTrueOrderByPriorityDescIdDesc();
}
