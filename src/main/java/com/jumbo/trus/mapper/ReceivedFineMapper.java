package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.entity.ReceivedFineEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {FineMapper.class, MatchMapper.class})
public abstract class ReceivedFineMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "playerId"),
            @Mapping(target = "match.id", source = "matchId"),
            @Mapping(target = "fine", source = "fine"),
    })
    public abstract ReceivedFineEntity toEntity(ReceivedFineDTO source);
    @Mappings({
            @Mapping(target = "playerId", source = "player.id"),
            @Mapping(target = "matchId", source = "match.id"),
            @Mapping(target = "fine", source = "fine"),
    })
    public abstract ReceivedFineDTO toDTO(ReceivedFineEntity source);
}
