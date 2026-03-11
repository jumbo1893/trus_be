package com.jumbo.trus.mapper.footbar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {MatchMapper.class, PlayerMapper.class})
public abstract class FootbarSessionMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Mapping(source = "location", target = "location", qualifiedByName = "mapLocation")
    @Mapping(source = "distance5Min", target = "distance5Min", qualifiedByName = "mapDistance5Min")
    @Mapping(source = "trackerData", target = "trackerData", qualifiedByName = "mapTrackerData")
    public abstract FootbarSessionDTO toDTO(FootbarSessionEntity entity);

    @Named("mapLocation")
    protected FootbarSessionDTO.LocationDTO mapLocation(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, FootbarSessionDTO.LocationDTO.class);
    }

    @Named("mapDistance5Min")
    protected java.util.List<FootbarSessionDTO.Distance5MinDTO> mapDistance5Min(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(
                value,
                objectMapper.getTypeFactory().constructCollectionType(
                        java.util.List.class,
                        FootbarSessionDTO.Distance5MinDTO.class
                )
        );
    }

    @Named("mapTrackerData")
    protected FootbarSessionDTO.TrackerDataDTO mapTrackerData(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, FootbarSessionDTO.TrackerDataDTO.class);
    }
}
