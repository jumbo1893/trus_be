package com.jumbo.trus.mapper.footbar;

import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.dto.footbar.api.FootbarSessionRawDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public final class FootbarRawSessionMapper {

    public static FootbarSessionDTO toDto(FootbarSessionRawDTO raw, MatchDTO match, PlayerDTO player) {
        if (raw == null) {
            return null;
        }

        FootbarSessionDTO dto = new FootbarSessionDTO();
        dto.setFootbarSessionId(raw.getFootbarSessionId());
        dto.setStartDate(convertStringToDate(raw.getStartDate()));
        dto.setStopDate(convertStringToDate(raw.getStopDate()));
        dto.setPlayingTime(raw.getPlayingTime());
        dto.setTitle(raw.getTitle());
        dto.setLocation(mapLocation(raw.getLocation()));
        dto.setMatchType(raw.getMatchType());
        dto.setPosition(raw.getPosition());
        dto.setScoreStars(raw.getScoreStars());
        dto.setDistance(raw.getDistance());
        dto.setPassCount(raw.getPassCount());
        dto.setShotCount(raw.getShotCount());
        dto.setShotSpeed(raw.getShotSpeed());
        dto.setAvgShotSpeed(raw.getAvgShotSpeed());
        dto.setDribbleCount(raw.getDribbleCount());
        dto.setTimeWithBall(raw.getTimeWithBall());
        dto.setActivity(raw.getActivity());
        dto.setTimeRunning(raw.getTimeRunning());
        dto.setRunCount(raw.getRunCount());
        dto.setSprintCount(raw.getSprintCount());
        dto.setAvgSprintSpeed(raw.getAvgSprintSpeed());
        dto.setSprintSpeed(raw.getSprintSpeed());
        dto.setHsrPlus(raw.getHsrPlus());
        dto.setStopAndGo(raw.getStopAndGo());
        dto.setAcceleration(raw.getAcceleration());
        dto.setDistance5Min(mapDistance5Min(raw.getDistance5Min()));
        dto.setTrackerData(mapTrackerData(raw.getTrackerData()));
        dto.setMatch(match);
        dto.setPlayer(player);

        return dto;
    }

    public FootbarSessionDTO toDto(FootbarSessionRawDTO raw) {
        return toDto(raw, null, null);
    }

    public static List<FootbarSessionDTO> toDtoList(List<FootbarSessionRawDTO> rawList, MatchDTO match, PlayerDTO player) {
        if (rawList == null) {
            return null;
        }

        return rawList.stream()
                .map(raw -> toDto(raw, match, player))
                .collect(Collectors.toList());
    }

    private static FootbarSessionDTO.LocationDTO mapLocation(FootbarSessionRawDTO.LocationDTO raw) {
        if (raw == null) {
            return null;
        }

        FootbarSessionDTO.LocationDTO dto = new FootbarSessionDTO.LocationDTO();
        dto.setType(raw.getType());
        dto.setCoordinates(raw.getCoordinates());
        return dto;
    }

    private static FootbarSessionDTO.TrackerDataDTO mapTrackerData(FootbarSessionRawDTO.TrackerDataDTO raw) {
        if (raw == null) {
            return null;
        }

        FootbarSessionDTO.TrackerDataDTO dto = new FootbarSessionDTO.TrackerDataDTO();
        dto.setTrackerMac(raw.getTrackerMac());
        dto.setTrackerName(raw.getTrackerName());
        return dto;
    }

    private static List<FootbarSessionDTO.Distance5MinDTO> mapDistance5Min(List<FootbarSessionRawDTO.Distance5MinDTO> rawList) {
        if (rawList == null) {
            return null;
        }

        return rawList.stream()
                .map(FootbarRawSessionMapper::mapDistance5MinItem)
                .collect(Collectors.toList());
    }

    private static FootbarSessionDTO.Distance5MinDTO mapDistance5MinItem(FootbarSessionRawDTO.Distance5MinDTO raw) {
        if (raw == null) {
            return null;
        }

        FootbarSessionDTO.Distance5MinDTO dto = new FootbarSessionDTO.Distance5MinDTO();
        dto.setIndex(raw.getIndex());
        dto.setDistance(mapDistanceSlice(raw.getDistance()));
        return dto;
    }

    private static FootbarSessionDTO.DistanceSliceDTO mapDistanceSlice(FootbarSessionRawDTO.DistanceSliceDTO raw) {
        if (raw == null) {
            return null;
        }

        FootbarSessionDTO.DistanceSliceDTO dto = new FootbarSessionDTO.DistanceSliceDTO();
        dto.setLow(raw.getLow());
        dto.setNormal(raw.getNormal());
        dto.setHigh(raw.getHigh());
        return dto;
    }

    private static Date convertStringToDate(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return null;
        }
        Instant instant = Instant.parse(dateTime);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Europe/Prague"));
        return Date.from(zonedDateTime.toInstant());
    }
}