package com.jumbo.trus.mapper.pkfl;

import com.jumbo.trus.dto.pkfl.PkflRefereeDTO;
import com.jumbo.trus.entity.pkfl.PkflRefereeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PkflMatchMapper.class})
public abstract class PkflRefereeMapper {

    @Mapping(target = "matchList", ignore = true)
    public abstract PkflRefereeEntity toEntity(PkflRefereeDTO source);
    public abstract PkflRefereeDTO toDTO(PkflRefereeEntity source);
}
