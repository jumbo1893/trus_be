package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.mapper.football.FootballPlayerMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {MatchMapper.class, BeerMapper.class, BeerDetailedMapper.class, ReceivedFineDetailedMapper.class, GoalMapper.class, FootballPlayerMapper.class})
public abstract class PlayerMapper {

    @Mappings({
            @Mapping(target = "matchList", ignore = true),
            @Mapping(target = "beerList", ignore = true),
            @Mapping(target = "fineList", ignore = true),
            @Mapping(target = "goalList", ignore = true),
            @Mapping(target = "appTeam", ignore = true),
            @Mapping(target = "userTeamRoles", ignore = true),
            @Mapping(target = "playerAchievements", ignore = true)
    })
    public abstract PlayerEntity toEntity(PlayerDTO source);
    public abstract PlayerDTO toDTO(PlayerEntity source);

}
