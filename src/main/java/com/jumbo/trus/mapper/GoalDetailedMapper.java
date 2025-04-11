package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.GoalEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.mapper.pkfl.PkflIndividualStatsMapper;
import com.jumbo.trus.mapper.pkfl.PkflMatchMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {PlayerMapper.class, MatchMapper.class, PkflIndividualStatsMapper.class, PkflMatchMapper.class, FootballMatchMapper.class})
public abstract class GoalDetailedMapper {

    @Mappings({
            @Mapping(target = "player.id", source = "player"),
            @Mapping(target = "match.id", source = "match"),
            @Mapping(target = "appTeam", ignore = true),
    })
    public abstract GoalEntity toEntity(GoalDetailedDTO source);

    @Mappings({
            @Mapping(target = "match.seasonId", source = "match.season.id"),
            @Mapping(target = "match.playerIdList", expression = "java(getPlayerIdsFromGoal(matchEntity))"),
    })
    public abstract GoalDetailedDTO toDTO(GoalEntity source);


    protected long map(PlayerDTO value) {
        return value.getId();
    }

    protected long map(MatchDTO value) {
        return value.getId();
    }

    protected List<Long> getPlayerIdsFromGoal(MatchEntity source){
        List<Long> result = new ArrayList<>();
        for(PlayerEntity player : source.getPlayerList()){
            result.add(player.getId());
        }
        return result;
    }

}
