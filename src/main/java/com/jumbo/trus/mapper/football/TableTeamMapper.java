package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.entity.football.TableTeamEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LeagueMapper.class, TeamMapper.class})

public abstract class TableTeamMapper {

    @Mapping(target = "team", ignore = true)
    public abstract TableTeamEntity toEntity(TableTeamDTO source);

    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    public abstract TableTeamDTO toDTO(TableTeamEntity source);
}
