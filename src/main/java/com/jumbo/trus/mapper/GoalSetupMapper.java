package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.goal.response.get.GoalSetupResponse;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.GoalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = PlayerMapper.class)
public abstract class GoalSetupMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "player"),
            @Mapping(target = "match", ignore = true),
            @Mapping(target = "appTeam", ignore = true),
    })
    public abstract GoalEntity toEntity(GoalSetupResponse source);

    @Mapping(target = "newGoalSetup", ignore = true)
    public abstract GoalSetupResponse toDTO(GoalEntity source);


    protected long map(PlayerDTO value) {
        return value.getId();
    }


}
