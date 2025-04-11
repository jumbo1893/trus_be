package com.jumbo.trus.entity.repository.achievement;

import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AchievementRepository extends JpaRepository<AchievementEntity, Long> {

    Optional<AchievementEntity> findByCode(String code);

}

