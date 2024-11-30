package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.FootballMatchPlayerDTO;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {FootballMatchMapper.class, FootballPlayerMapper.class, TeamMapper.class})
public abstract class FootballMatchPlayerMapper {

    @Mapping(target = "match.id", source = "matchId")
    public abstract FootballMatchPlayerEntity toEntity(FootballMatchPlayerDTO source);
    @Mapping(target = "matchId", source = "match.id")
    public abstract FootballMatchPlayerDTO toDTO(FootballMatchPlayerEntity source);
}
