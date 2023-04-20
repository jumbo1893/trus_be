package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.MatchDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {BeerMapper.class, BeerDetailedMapper.class, ReceivedFineDetailedMapper.class})
public abstract class MatchMapper {

    @Mappings({
            @Mapping(target = "season.id", source = "seasonId"),
            //@Mapping(target = "playerList", expression = "java(getPlayerIds(source))"),
    })
    public abstract MatchEntity toEntity(MatchDTO source);
    @Mappings({
            @Mapping(target = "seasonId", source = "season.id"),
            @Mapping(target = "playerIdList", expression = "java(getPlayerIds(source))"),
    })
    public abstract MatchDTO toDTO(MatchEntity source);

    protected List<Long> getPlayerIds(MatchEntity source){
        List<Long> result = new ArrayList<>();
        for(PlayerEntity player : source.getPlayerList()){
            result.add(player.getId());
        }
        return result;
    }
}
