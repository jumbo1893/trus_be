package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.entity.football.TableTeamEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {LeagueMapper.class, TeamMapper.class})

public abstract class TableTeamMapper {

    public abstract TableTeamEntity toEntity(TableTeamDTO source);

    public abstract TableTeamDTO toDTO(TableTeamEntity source);
}
