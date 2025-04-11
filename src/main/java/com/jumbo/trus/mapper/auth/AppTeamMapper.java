package com.jumbo.trus.mapper.auth;

import com.jumbo.trus.dto.auth.AppTeamDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.football.TeamMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, TeamMapper.class, UserTeamRoleMapper.class})
public abstract class AppTeamMapper {

    @Mapping(target = "teamRoles", ignore = true)
    @Mapping(target = "playerList", ignore = true)
    @Mapping(target = "matchList", ignore = true)
    @Mapping(target = "notificationList", ignore = true)
    @Mapping(target = "goalList", ignore = true)
    @Mapping(target = "receivedFineList", ignore = true)
    @Mapping(target = "seasonList", ignore = true)
    @Mapping(target = "fineList", ignore = true)
    @Mapping(target = "beerList", ignore = true)
    @Mapping(target = "owner", ignore = true)
    public abstract AppTeamEntity toEntity(AppTeamDTO source);

    @Mapping(target = "ownerId", source = "owner.id")
    public abstract AppTeamDTO toDTO(AppTeamEntity source);
}
