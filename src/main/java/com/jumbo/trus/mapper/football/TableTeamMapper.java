package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.entity.football.TableTeamEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {LeagueMapper.class, TeamMapper.class})

public abstract class TableTeamMapper {

    @Mappings({
            @Mapping(target = "league.id", source = "leagueId"),
            @Mapping(target = "team.id", source = "teamId"),
    })
    public abstract TableTeamEntity toEntity(TableTeamDTO source);

    @Mappings({
            @Mapping(target = "leagueId", source = "league.id"),
            @Mapping(target = "teamId", source = "team.id"),
    })
    public abstract TableTeamDTO toDTO(TableTeamEntity source);
}
