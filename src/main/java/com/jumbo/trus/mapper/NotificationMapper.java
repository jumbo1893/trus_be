package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.NotificationDTO;
import com.jumbo.trus.entity.NotificationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class NotificationMapper {

    public abstract NotificationEntity toEntity(NotificationDTO source);

    public abstract NotificationDTO toDTO(NotificationEntity source);
}
