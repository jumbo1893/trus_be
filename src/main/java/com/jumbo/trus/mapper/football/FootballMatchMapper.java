package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.FootballMatchPlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import com.jumbo.trus.mapper.MatchMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {MatchMapper.class, FootballMatchPlayerMapper.class, TeamMapper.class, LeagueMapper.class})
public abstract class FootballMatchMapper {

    @Autowired
    private FootballMatchPlayerMapper footballMatchPlayerMapper;

    @Mappings({
            @Mapping(target = "matchList", ignore = true),
            //@Mapping(target = "homePlayerList", ignore = true),
            //@Mapping(target = "awayPlayerList", ignore = true),
    })
    public abstract FootballMatchEntity toEntity(FootballMatchDTO source);

    @Mappings({
            @Mapping(target = "matchIdList", expression = "java(getMatchIds(source))"),
            @Mapping(target = "homePlayerList", expression = "java(getHomePlayerList(source))"),
            @Mapping(target = "awayPlayerList", expression = "java(getAwayPlayerList(source))"),
            @Mapping(target = "playerList", ignore = true),
    })
    public abstract FootballMatchDTO toDTO(FootballMatchEntity source);

    protected List<Long> getMatchIds(FootballMatchEntity source){
        List<Long> result = new ArrayList<>();
        if (source.getMatchList() != null) {
            for (MatchEntity match : source.getMatchList()) {
                result.add(match.getId());
            }
        }
        return result;
    }

    protected List<FootballMatchPlayerDTO> getHomePlayerList(FootballMatchEntity source) {
        List<FootballMatchPlayerDTO> result = new ArrayList<>();
        if (source.getPlayerList() != null) {
            for (FootballMatchPlayerEntity player : source.getPlayerList()) {
                if (player.getTeam().getId().equals(source.getHomeTeam().getId())) {
                    result.add(footballMatchPlayerMapper.toDTO(player));
                }
            }
        }
        return result;
    }

    protected List<FootballMatchPlayerDTO> getAwayPlayerList(FootballMatchEntity source) {
        List<FootballMatchPlayerDTO> result = new ArrayList<>();
        if (source.getPlayerList() != null) {
            for (FootballMatchPlayerEntity player : source.getPlayerList()) {
                if (player.getTeam().getId().equals(source.getAwayTeam().getId())) {
                    result.add(footballMatchPlayerMapper.toDTO(player));
                }
            }
        }
        return result;
    }

}
