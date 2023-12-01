package com.jumbo.trus.mapper.pkfl;

import com.jumbo.trus.dto.pkfl.PkflIndividualStatsDTO;
import com.jumbo.trus.entity.pkfl.PkflIndividualStatsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PkflMatchMapper.class, PkflPlayerMapper.class})
public abstract class PkflIndividualStatsMapper {

    @Mapping(target = "match.id", source = "matchId")
    public abstract PkflIndividualStatsEntity toEntity(PkflIndividualStatsDTO source);
    @Mapping(target = "matchId", source = "match.id")
    public abstract PkflIndividualStatsDTO toDTO(PkflIndividualStatsEntity source);
}
