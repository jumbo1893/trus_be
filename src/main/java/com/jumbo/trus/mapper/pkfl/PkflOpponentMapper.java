package com.jumbo.trus.mapper.pkfl;

import com.jumbo.trus.dto.pkfl.PkflOpponentDTO;
import com.jumbo.trus.entity.pkfl.PkflOpponentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PkflMatchMapper.class})
public abstract class PkflOpponentMapper {

    @Mapping(target = "matchList", ignore = true)
    public abstract PkflOpponentEntity toEntity(PkflOpponentDTO source);
    public abstract PkflOpponentDTO toDTO(PkflOpponentEntity source);
}
