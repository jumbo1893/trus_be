package com.jumbo.trus.entity.repository.football;

import com.jumbo.trus.entity.football.FootballPlayerEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FootballPlayerRepository extends JpaRepository<FootballPlayerEntity, Long> {

    boolean existsByUri(String uri);

    FootballPlayerEntity findByUri(String uri);

    @Modifying
    @Transactional
    @Query(value = "UPDATE football_player SET name = :name, birthYear = :birthYear, email = :email, phoneNumber = :phoneNumber WHERE id = :id", nativeQuery = true)
    int updatePlayerFields(@Param("id") Long id,
                         @Param("name") String name,
                         @Param("birthYear") int birthYear,
                         @Param("email") String email,
                         @Param("phoneNumber") String phoneNumber);

    @Query(value = "SELECT football_player_id FROM team_players WHERE team_id = :teamId", nativeQuery = true)
    List<Long> findAllPlayersIdsByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM team_players WHERE team_id = :teamId", nativeQuery = true)
    List<Long> deleteTeamPlayers(@Param("teamId") Long teamId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM team_players WHERE team_id = :teamId AND player_id = :playerId", nativeQuery = true)
    void deleteTeamPlayers(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO public.team_players (team_id, football_player_id) VALUES (:teamId, :playerId)", nativeQuery = true)
    void saveNewTeamPlayer(@Param("teamId") Long teamId, @Param("playerId") Long playerId);
}
