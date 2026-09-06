package com.jumbo.trus.repository.ai;

import com.jumbo.trus.entity.ai.MatchReportEntity;
import com.jumbo.trus.entity.ai.MatchReportStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MatchReportRepository extends JpaRepository<MatchReportEntity, Long> {
    boolean existsByAppTeamIdAndFootballMatchId(Long appTeamId, Long footballMatchId);
    Optional<MatchReportEntity> findFirstByAppTeamIdAndFootballMatchIdAndStyleOrderByIdDesc(
            Long appTeamId, Long footballMatchId, MatchReportStyle style);
}
