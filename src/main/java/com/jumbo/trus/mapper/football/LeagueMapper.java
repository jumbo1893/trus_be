package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.entity.football.LeagueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public abstract class LeagueMapper {

    @Mappings({
            @Mapping(target = "teamList", ignore = true),
            @Mapping(target = "tableTeamList", ignore = true),
    })
    public abstract LeagueEntity toEntity(LeagueDTO source);

    @Mappings({
            @Mapping(target = "tableTeamIdList", ignore = true),
    })
    public abstract LeagueDTO toDTO(LeagueEntity source);

    /*protected List<Long> getTableTeamsId(LeagueEntity source){
        List<Long> result = new ArrayList<>();
        for(TableTeamEntity tableTeam : source.getTableTeamList()){
            result.add(tableTeam.getId());
        }
        return result;
    }*/
}
