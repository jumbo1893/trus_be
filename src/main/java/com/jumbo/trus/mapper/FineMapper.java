package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.SeasonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ReceivedFineDetailedMapper.class})
public abstract class FineMapper {

    @Mapping(target = "receivedFineList", ignore = true)
    @Mapping(target = "editable", ignore = true)
    public abstract FineEntity toEntity(FineDTO source);
    public abstract FineDTO toDTO(FineEntity source);
}
