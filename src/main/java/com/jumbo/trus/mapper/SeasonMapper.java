package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.SeasonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MatchMapper.class, BeerDetailedMapper.class})
public abstract class SeasonMapper {

    @Mapping(target = "matchList", ignore = true)
    @Mapping(target = "editable", ignore = true)
    @Mapping(target = "appTeam", ignore = true)
    public abstract SeasonEntity toEntity(SeasonDTO source);
    public abstract SeasonDTO toDTO(SeasonEntity source);
}
