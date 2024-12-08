package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.detail.BestScorer;
import com.jumbo.trus.entity.football.detail.BestScorerView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TeamMapper.class, FootballPlayerMapper.class, LeagueMapper.class})

public abstract class BestScorerViewMapper {

    public abstract BestScorer toDTO(BestScorerView source);
}
