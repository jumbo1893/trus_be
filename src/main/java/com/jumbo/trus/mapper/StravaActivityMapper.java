package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.strava.StravaActivity;
import com.jumbo.trus.entity.strava.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class StravaActivityMapper {

    @Mapping(source = "distanceKm", target = "distance", qualifiedByName = "kmToMeters")
    @Mapping(source = "durationSeconds", target = "elapsedTime")
    @Mapping(source = "movingTimeSeconds", target = "movingTime")
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    public abstract StravaActivity toDTO(ActivityEntity entity);

    @Named("kmToMeters")
    static Float kmToMeters(Float km) {
        return km != null ? km * 1000 : null;
    }
}
