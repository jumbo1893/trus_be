package com.jumbo.trus.mapper.auth;

import com.jumbo.trus.dto.auth.AppTeamDTO;
import com.jumbo.trus.dto.auth.UserTeamRoleDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.football.TeamMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, TeamMapper.class, AppTeamMapper.class, PlayerMapper.class})
public abstract class UserTeamRoleMapper {

    @Mapping(target = "user", ignore = true)
    public abstract UserTeamRole toEntity(UserTeamRoleDTO source);

    @Mapping(target = "userId", source = "user.id")
    public abstract UserTeamRoleDTO toDTO(UserTeamRole source);
}
