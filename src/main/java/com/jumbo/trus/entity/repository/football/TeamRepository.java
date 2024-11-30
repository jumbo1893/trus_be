package com.jumbo.trus.entity.repository.football;

import com.jumbo.trus.entity.football.TeamEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    boolean existsByUri(String uri);

    TeamEntity findByUri(String uri);

    @Modifying
    @Transactional
    @Query(value = "UPDATE team SET name = :name, current_league_id = :currentLeagueId WHERE id = :id", nativeQuery = true)
    int updateTeamFields(@Param("id") Long id,
                           @Param("name") String name,
                           @Param("currentLeagueId") Long currentLeagueId);

    @Query(value = "SELECT * FROM team WHERE current_league_id in (SELECT id FROM league WHERE current_league = true)", nativeQuery = true)
    List<TeamEntity> findTeamsFromCurrentSeason();
}
