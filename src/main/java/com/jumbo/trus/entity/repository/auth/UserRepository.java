package com.jumbo.trus.entity.repository.auth;

import com.jumbo.trus.entity.auth.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByMail(String userName);

    List<UserEntity> findDistinctByTeamRoles_AppTeam_Id(Long appTeamId);

    @Query(value = "SELECT player_id from auth WHERE id=:#{#id}", nativeQuery = true)
    Long findPlayerId(@Param("id") Long id);


}

