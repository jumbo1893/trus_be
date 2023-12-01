package com.jumbo.trus.mapper.pkfl;

import com.jumbo.trus.dto.pkfl.PkflMatchDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.pkfl.PkflMatchEntity;
import com.jumbo.trus.mapper.MatchMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {PkflStadiumMapper.class, PkflSeasonMapper.class, PkflRefereeMapper.class, MatchMapper.class, PkflIndividualStatsMapper.class, PkflOpponentMapper.class})
public abstract class PkflMatchMapper {

    @Mapping(target = "matchList", ignore = true)
    public abstract PkflMatchEntity toEntity(PkflMatchDTO source);

    @Mappings({
            @Mapping(target = "matchIdList", expression = "java(getMatchIds(source))"),
    })
    public abstract PkflMatchDTO toDTO(PkflMatchEntity source);

    protected List<Long> getMatchIds(PkflMatchEntity source){
        List<Long> result = new ArrayList<>();
        for(MatchEntity match : source.getMatchList()){
            result.add(match.getId());
        }
        return result;
    }

}
