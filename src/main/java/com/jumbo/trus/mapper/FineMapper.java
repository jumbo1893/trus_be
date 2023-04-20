package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.SeasonEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ReceivedFineDetailedMapper.class})
public abstract class FineMapper {

    public abstract FineEntity toEntity(FineDTO source);
    public abstract FineDTO toDTO(FineEntity source);
}
