package com.jumbo.trus.repository.footbar;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FootbarAccountRepository extends JpaRepository<FootbarAccountEntity, Long> {
  Optional<FootbarAccountEntity> findByUserId(Long userId);
  Optional<FootbarAccountEntity> findByFootbarUserId(Long footbarUserId);

  @Query("""
    SELECT a FROM FootbarAccountEntity a
    WHERE a.user IN (
        SELECT r.user FROM UserTeamRole r WHERE r.appTeam = :appTeam
    )
""")
  List<FootbarAccountEntity> findAllAccountsByAppTeam(@Param("appTeam") AppTeamEntity appTeam);

  List<FootbarAccountEntity> findAllByFootbarUserId(Long footbarUserId);

  void deleteByFootbarUserId(Long footbarUserId);
}
