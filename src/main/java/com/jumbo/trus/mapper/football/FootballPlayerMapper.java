package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TeamMapper.class})

public abstract class FootballPlayerMapper {

    @Mappings({
            @Mapping(target = "teamList", ignore = true),
    })
    public abstract FootballPlayerEntity toEntity(FootballPlayerDTO source);


    @Mappings({
            @Mapping(target = "teamIdList", ignore = true),
    })
    public abstract FootballPlayerDTO toDTO(FootballPlayerEntity source);
}
