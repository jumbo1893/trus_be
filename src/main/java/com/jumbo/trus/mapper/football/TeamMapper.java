package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.football.TeamEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring", uses = {LeagueMapper.class, TableTeamMapper.class, FootballPlayerMapper.class, FootballMatchMapper.class})

public abstract class TeamMapper {

    @Mappings({
            @Mapping(target = "currentLeague.id", source = "currentLeagueId"),
            @Mapping(target = "tableTeamList", ignore = true),
            @Mapping(target = "leagueList", ignore = true),
            @Mapping(target = "homeMatchList", ignore = true),
            @Mapping(target = "awayMatchList", ignore = true),
            @Mapping(target = "footballMatchPlayers", ignore = true),
    })
    public abstract TeamEntity toEntity(TeamDTO source);

    @Mappings({
            @Mapping(target = "currentLeagueId", source = "currentLeague.id"),
            @Mapping(target = "footballPlayerList", ignore = true),
            @Mapping(target = "tableTeamIdList", ignore = true),
            @Mapping(target = "currentTableTeam", ignore = true)
    })
    public abstract TeamDTO toDTO(TeamEntity source);
}
