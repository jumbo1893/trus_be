package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.notification.NotificationDTO;
import com.jumbo.trus.entity.notification.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class NotificationMapper {

    @Mapping(target = "appTeam", ignore = true)
    public abstract NotificationEntity toEntity(NotificationDTO source);

    public abstract NotificationDTO toDTO(NotificationEntity source);
}
