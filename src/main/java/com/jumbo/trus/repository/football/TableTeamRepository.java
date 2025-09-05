package com.jumbo.trus.repository.football;

import com.jumbo.trus.entity.football.TableTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TableTeamRepository extends JpaRepository<TableTeamEntity, Long> {

    TableTeamEntity findByTeamIdAndLeagueId(Long teamId, Long leagueId);

    List<TableTeamEntity> findByLeagueIdOrderByPointsDesc(Long leagueId);
}
