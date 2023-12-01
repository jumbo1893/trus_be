package com.jumbo.trus.mapper.pkfl;

import com.jumbo.trus.dto.pkfl.PkflPlayerDTO;
import com.jumbo.trus.entity.pkfl.PkflPlayerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PkflMatchMapper.class})
public abstract class PkflPlayerMapper {


    @Mapping(target = "individualStatsList", ignore = true)
    public abstract PkflPlayerEntity toEntity(PkflPlayerDTO source);
    public abstract PkflPlayerDTO toDTO(PkflPlayerEntity source);
}
