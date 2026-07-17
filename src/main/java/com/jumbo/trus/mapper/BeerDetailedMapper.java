package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.mapper.pkfl.PkflIndividualStatsMapper;
import com.jumbo.trus.mapper.pkfl.PkflMatchMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(
        componentModel = "spring",
        uses = {
                MatchMapper.class,
                PlayerMapper.class,
                PkflIndividualStatsMapper.class,
                PkflMatchMapper.class,
                FootballMatchMapper.class
        }
)
public abstract class BeerDetailedMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "player"),
            @Mapping(target = "match.id", source = "match"),
            @Mapping(target = "appTeam", ignore = true)
    })
    public abstract BeerEntity toEntity(BeerDetailedDTO source);

    public abstract BeerDetailedDTO toDTO(BeerEntity source);

    protected long map(PlayerDTO value) {
        return value.getId();
    }

    protected long map(MatchDTO value) {
        return value.getId();
    }
}