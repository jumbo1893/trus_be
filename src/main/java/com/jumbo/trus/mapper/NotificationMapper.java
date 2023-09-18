package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.NotificationDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.NotificationEntity;
import com.jumbo.trus.entity.PlayerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class NotificationMapper {

    public abstract NotificationEntity toEntity(NotificationDTO source);

    public abstract NotificationDTO toDTO(NotificationEntity source);
}
