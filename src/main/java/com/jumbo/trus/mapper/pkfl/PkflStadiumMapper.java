package com.jumbo.trus.mapper.pkfl;

import com.jumbo.trus.dto.pkfl.PkflStadiumDTO;
import com.jumbo.trus.entity.pkfl.PkflStadiumEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PkflMatchMapper.class})
public abstract class PkflStadiumMapper {

    @Mapping(target = "matchList", ignore = true)
    public abstract PkflStadiumEntity toEntity(PkflStadiumDTO source);
    public abstract PkflStadiumDTO toDTO(PkflStadiumEntity source);
}
