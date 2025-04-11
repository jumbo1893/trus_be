package com.jumbo.trus.mapper.achievement;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {AchievementMapper.class, PlayerMapper.class, MatchMapper.class, FootballMatchMapper.class})
public abstract class PlayerAchievementMapper {

    public abstract PlayerAchievementEntity toEntity(PlayerAchievementDTO source);

    public abstract PlayerAchievementDTO toDTO(PlayerAchievementEntity source);
}
