package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalSetupResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.GoalEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = PlayerMapper.class)
public abstract class GoalSetupMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "player"),
            @Mapping(target = "match", ignore = true)
    })
    public abstract GoalEntity toEntity(GoalSetupResponse source);

    @Mapping(target = "newGoalSetup", ignore = true)
    public abstract GoalSetupResponse toDTO(GoalEntity source);


    protected long map(PlayerDTO value) {
        return value.getId();
    }


}
