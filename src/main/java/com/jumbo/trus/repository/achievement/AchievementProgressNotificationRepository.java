package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.achievement.AchievementProgressNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AchievementProgressNotificationRepository
        extends JpaRepository<AchievementProgressNotificationEntity, Long> {

    List<AchievementProgressNotificationEntity> findAllByPlayerAchievementIdIn(
            Collection<Long> playerAchievementIds
    );
}
