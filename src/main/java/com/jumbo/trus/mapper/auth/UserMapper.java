package com.jumbo.trus.mapper.auth;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.mapper.ReceivedFineDetailedMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ReceivedFineDetailedMapper.class, UserTeamRoleMapper.class, AppTeamMapper.class})
public abstract class UserMapper {

    @Mapping(target = "appTeamsOwner", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "athletes", ignore = true)
    @Mapping(target = "deviceTokens", ignore = true)
    public abstract UserEntity toEntity(UserDTO source);

    @Mapping(target = "password", ignore = true)
    public abstract UserDTO toDTO(UserEntity source);
}
