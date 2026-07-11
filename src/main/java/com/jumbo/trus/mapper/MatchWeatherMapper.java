package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.weather.MatchWeatherDTO;
import com.jumbo.trus.entity.MatchWeatherEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchWeatherMapper {

    @Mapping(target = "matchId", ignore = true)
    @Mapping(target = "match", ignore = true)
    MatchWeatherEntity toEntity(MatchWeatherDTO source);

    MatchWeatherDTO toDTO(MatchWeatherEntity source);
}
