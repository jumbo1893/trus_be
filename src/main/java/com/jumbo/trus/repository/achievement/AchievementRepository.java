package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.achievement.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<AchievementEntity, Long> {

    @Query("""
            SELECT DISTINCT achievement
            FROM AchievementEntity achievement
            LEFT JOIN FETCH achievement.achievementTypes
            """)
    List<AchievementEntity> findAllWithTypes();

    Optional<AchievementEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<AchievementEntity> findAllByCodeIn(
            Collection<String> codes
    );

}

