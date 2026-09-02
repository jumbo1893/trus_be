package com.jumbo.trus.repository.auth;

import com.jumbo.trus.entity.auth.TeamJoinCodeEntity;
import com.jumbo.trus.entity.auth.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamJoinCodeRepository extends JpaRepository<TeamJoinCodeEntity, Long> {

    Optional<TeamJoinCodeEntity> findByCode(String code);

    Optional<TeamJoinCodeEntity> findByAppTeamIdAndGrantedRole(Long appTeamId, TeamRole grantedRole);

    boolean existsByCode(String code);
}
