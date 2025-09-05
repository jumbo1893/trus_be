package com.jumbo.trus.repository.football;

import com.jumbo.trus.entity.football.FootballPlayerEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FootballPlayerRepository extends JpaRepository<FootballPlayerEntity, Long> {

    boolean existsByUri(String uri);

    FootballPlayerEntity findByUri(String uri);

    @Query("SELECT p FROM football_player p JOIN p.teamList t WHERE t.id = :teamId ORDER BY p.name")
    List<FootballPlayerEntity> findAllByTeamId(@Param("teamId") Long teamId);

    @Query(value = "SELECT * FROM football_player WHERE id IN (SELECT player_id FROM football_match_player WHERE team_id = :teamId) ORDER BY name", nativeQuery = true)
    List<FootballPlayerEntity> findAllByTeamIdWithInactive(@Param("teamId") Long teamId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE football_player SET name = :name, birthYear = :birthYear, email = :email, phoneNumber = :phoneNumber WHERE id = :id")
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
    @Query(value = "DELETE FROM team_players WHERE team_id = :teamId AND football_player_id = :playerId", nativeQuery = true)
    void deleteTeamPlayers(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO public.team_players (team_id, football_player_id) VALUES (:teamId, :playerId)", nativeQuery = true)
    void saveNewTeamPlayer(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    @Query(value = "SELECT COALESCE(AVG(fp.birth_year), 0) " +
            "FROM football_player fp " +
            "JOIN team_players tp ON fp.id = tp.football_player_id " +
            "WHERE tp.team_id = :teamId",
            nativeQuery = true)
    Optional<Double> findAverageBirthYearByTeam(@Param("teamId") Long teamId);
}
