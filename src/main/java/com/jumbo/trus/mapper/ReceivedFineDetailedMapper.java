package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.mapper.pkfl.PkflIndividualStatsMapper;
import com.jumbo.trus.mapper.pkfl.PkflMatchMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {MatchMapper.class, PkflMatchMapper.class, PkflIndividualStatsMapper.class, FootballMatchMapper.class})
public abstract class ReceivedFineDetailedMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "player"),
            @Mapping(target = "match.id", source = "match"),
            @Mapping(target = "fine.id", source = "fine"),
    })
    public abstract ReceivedFineEntity toEntity(ReceivedFineDetailedDTO source);

    @Mappings({
            @Mapping(target = "match.seasonId", source = "match.season.id"),
            @Mapping(target = "match.playerIdList", expression = "java(getPlayerIdsFromFine(matchEntity))"),
            @Mapping(target = "fineAmount", expression = "java(source.getFineNumber() * source.getFine().getAmount())")
    })
    public abstract ReceivedFineDetailedDTO toDTO(ReceivedFineEntity source);


    protected long map(PlayerDTO value) {
        return value.getId();
    }

    protected long map(MatchDTO value) {
        return value.getId();
    }

    protected long map(FineDTO value) {
        return value.getId();
    }

    protected List<Long> getPlayerIdsFromFine(MatchEntity source){
        List<Long> result = new ArrayList<>();
        for(PlayerEntity player : source.getPlayerList()){
            result.add(player.getId());
        }
        return result;
    }

}
