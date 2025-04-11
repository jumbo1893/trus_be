package com.jumbo.trus.mapper.football;

import com.jumbo.trus.dto.football.detail.BestScorer;
import com.jumbo.trus.entity.football.view.BestScorerEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TeamMapper.class, FootballPlayerMapper.class, LeagueMapper.class})

public abstract class BestScorerViewMapper {

    public abstract BestScorer toDTO(BestScorerEntity source);
}
