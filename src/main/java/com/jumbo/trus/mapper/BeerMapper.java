package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.entity.BeerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {PlayerMapper.class, MatchMapper.class})
public abstract class BeerMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "playerId"),
            @Mapping(target = "match.id", source = "matchId"),
            @Mapping(target = "appTeam", ignore = true),
    })
    public abstract BeerEntity toEntity(BeerDTO source);
    @Mappings({
            @Mapping(target = "playerId", source = "player.id"),
            @Mapping(target = "matchId", source = "match.id"),
    })
    public abstract BeerDTO toDTO(BeerEntity source);

}
