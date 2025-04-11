package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.stats.FootballSumIndividualStats;
import com.jumbo.trus.entity.football.view.FootballSumIndividualStatsEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TeamMapper.class, FootballPlayerMapper.class, LeagueMapper.class})

public abstract class FootballSumIndividualStatsMapper {

    public abstract FootballSumIndividualStats toDTO(FootballSumIndividualStatsEntity source);
}
