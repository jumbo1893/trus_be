package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.weather.MatchWeatherDTO;
import com.jumbo.trus.entity.MatchWeatherEntity;
import com.jumbo.trus.entity.weather.WeatherCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MatchWeatherMapper {

    /**
     * DTO -> entita.
     *
     * WeatherCode enum se převede zpět na číselný kód,
     * který je uložený v databázi.
     */
    @Mapping(target = "matchId", ignore = true)
    @Mapping(target = "match", ignore = true)
    @Mapping(target = "footballMatchId", ignore = true)
    @Mapping(
            target = "weatherCode",
            source = "weatherCode",
            qualifiedByName = "weatherCodeToInteger"
    )
    MatchWeatherEntity toEntity(MatchWeatherDTO source);


    @Mapping(
            target = "weatherCode",
            source = "weatherCode",
            qualifiedByName = "integerToWeatherCode"
    )
    MatchWeatherDTO toDTO(MatchWeatherEntity source);

    @Named("integerToWeatherCode")
    default WeatherCode integerToWeatherCode(Integer code) {
        return WeatherCode.fromCode(code);
    }

    @Named("weatherCodeToInteger")
    default Integer weatherCodeToInteger(
            WeatherCode weatherCode
    ) {
        if (weatherCode == null) {
            return null;
        }

        return weatherCode.getCode();
    }
}