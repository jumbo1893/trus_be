package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.NotificationDTO;
import com.jumbo.trus.dto.UpdateDTO;
import com.jumbo.trus.entity.NotificationEntity;
import com.jumbo.trus.entity.UpdateEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class UpdateMapper {

    public abstract UpdateEntity toEntity(UpdateDTO source);

    public abstract UpdateDTO toDTO(UpdateEntity source);
}
