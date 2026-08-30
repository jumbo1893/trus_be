package com.jumbo.trus.repository.auth;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByMail(String userName);

    Optional<UserEntity> findByMailIgnoreCase(String mail);

    @EntityGraph(attributePaths = {"teamRoles", "teamRoles.appTeam"})
    Optional<UserEntity> findByFirebaseUid(String firebaseUid);

    @EntityGraph(attributePaths = {"teamRoles", "teamRoles.appTeam"})
    Optional<UserEntity> findWithTeamRolesByMailIgnoreCase(String mail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT user FROM UserEntity user WHERE user.id = :userId")
    Optional<UserEntity> findByIdForUpdate(@Param("userId") Long userId);

    List<UserEntity> findDistinctByTeamRoles_AppTeam_Id(Long appTeamId);

    @Query(value = "SELECT player_id from auth WHERE id=:#{#id}", nativeQuery = true)
    Long findPlayerId(@Param("id") Long id);


}

