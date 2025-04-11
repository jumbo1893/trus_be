package com.jumbo.trus.mapper.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {})
public abstract class AchievementMapper {

    @Mapping(target = "playerAchievements", ignore = true)
    public abstract AchievementEntity toEntity(AchievementDTO source);

    public abstract AchievementDTO toDTO(AchievementEntity source);
}
