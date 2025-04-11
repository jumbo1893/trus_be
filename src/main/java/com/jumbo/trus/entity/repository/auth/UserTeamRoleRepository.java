package com.jumbo.trus.entity.repository.auth;

import com.jumbo.trus.entity.auth.UserTeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTeamRoleRepository extends JpaRepository<UserTeamRole, Long> {

    Optional<UserTeamRole> findByUserIdAndAppTeamId(Long userId, Long appTeamId);

}

