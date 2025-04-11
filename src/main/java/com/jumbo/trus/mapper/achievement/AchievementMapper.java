package com.jumbo.trus.mapper.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.auth.AppTeamDTO;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.auth.UserMapper;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.mapper.football.TeamMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {})
public abstract class AchievementMapper {

    @Mapping(target = "playerAchievements", ignore = true)
    public abstract AchievementEntity toEntity(AchievementDTO source);

    public abstract AchievementDTO toDTO(AchievementEntity source);
}
