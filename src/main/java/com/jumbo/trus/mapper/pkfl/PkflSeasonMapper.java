package com.jumbo.trus.mapper.pkfl;

import com.jumbo.trus.dto.pkfl.PkflSeasonDTO;
import com.jumbo.trus.entity.pkfl.PkflSeasonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PkflMatchMapper.class})
public abstract class PkflSeasonMapper {

    @Mapping(target = "matchList", ignore = true)
    public abstract PkflSeasonEntity toEntity(PkflSeasonDTO source);
    public abstract PkflSeasonDTO toDTO(PkflSeasonEntity source);
}
