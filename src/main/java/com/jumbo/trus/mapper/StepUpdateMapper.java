package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.StepUpdateDTO;
import com.jumbo.trus.entity.StepUpdateEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class StepUpdateMapper {

    public abstract StepUpdateEntity toEntity(StepUpdateDTO source);

    public abstract StepUpdateDTO toDTO(StepUpdateEntity source);
}
