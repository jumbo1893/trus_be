package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.entity.GoalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {PlayerMapper.class, MatchMapper.class})
public abstract class GoalMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "playerId"),
            @Mapping(target = "match.id", source = "matchId"),
            @Mapping(target = "appTeam", ignore = true),
    })
    public abstract GoalEntity toEntity(GoalDTO source);
    @Mappings({
            @Mapping(target = "playerId", source = "player.id"),
            @Mapping(target = "matchId", source = "match.id"),
    })
    public abstract GoalDTO toDTO(GoalEntity source);

}
