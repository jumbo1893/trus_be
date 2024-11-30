package com.jumbo.trus.entity.repository.football;

import com.jumbo.trus.entity.football.FootballMatchEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FootballMatchRepository extends JpaRepository<FootballMatchEntity, Long> {

    Optional<FootballMatchEntity> findByHomeTeam_IdAndRoundAndLeagueId(Long homeTeamId, Integer round, Long leagueId);

    @Transactional
    @Modifying
    @Query("DELETE FROM football_match f " +
            "WHERE f.leagueId = :leagueId AND f.id NOT IN :ids")
    int deleteByLeagueIdAndMatchIdNotIn(@Param("leagueId") Long leagueId,
                                             @Param("ids") List<Long> ids);
}
