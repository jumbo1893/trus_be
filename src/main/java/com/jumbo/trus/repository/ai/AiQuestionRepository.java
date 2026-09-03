package com.jumbo.trus.repository.ai;

import com.jumbo.trus.entity.ai.AiQuestionEntity;
import com.jumbo.trus.entity.ai.AiQuestionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AiQuestionRepository extends JpaRepository<AiQuestionEntity, Long> {

    long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            Instant from,
            Instant to
    );

    List<AiQuestionEntity> findByUserIdAndAppTeamIdOrderByCreatedAtDesc(
            Long userId,
            Long appTeamId,
            Pageable pageable
    );

    List<AiQuestionEntity> findByUserIdAndAppTeamIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            Long appTeamId,
            AiQuestionStatus status,
            Pageable pageable
    );
}
