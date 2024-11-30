package com.jumbo.trus.entity.repository.football;

import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FootballMatchPlayerRepository extends JpaRepository<FootballMatchPlayerEntity, Long> {

    @Query("SELECT f.id " +
            "FROM football_match_player f " +
            "WHERE f.team.id = :teamId AND f.match.id = :matchId AND f.player.id = :playerId")
    Optional<Long> findFirstIdByTeamAndMatchAndPlayer(@Param("teamId") Long teamId,
                                                      @Param("matchId") Long matchId,
                                                      @Param("playerId") Long playerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM football_match_player f " +
            "WHERE f.match.id = :matchId AND f.id NOT IN :ids")
    int deleteByMatchIdAndMatchPlayerIdNotIn(@Param("matchId") Long matchId,
                                        @Param("ids") List<Long> ids);
}
