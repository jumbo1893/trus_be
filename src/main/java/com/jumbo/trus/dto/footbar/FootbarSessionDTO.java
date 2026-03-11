package com.jumbo.trus.dto.footbar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FootbarSessionDTO {

    /**
     * Interní DB id
     */
    private Long id;

    /**
     * ID session z Footbar API
     */
    private Long footbarSessionId;

    private Date startDate;
    private Date stopDate;
    private Double playingTime;
    private String title;
    private LocationDTO location;
    private String matchType;
    private String position;
    private Double scoreStars;
    private Double distance;
    private Integer passCount;
    private Integer shotCount;
    private Double shotSpeed;
    private Double avgShotSpeed;
    private Integer dribbleCount;
    private Double timeWithBall;
    private Double activity;
    private Double timeRunning;
    private Double runCount;
    private Integer sprintCount;
    private Double avgSprintSpeed;
    private Double sprintSpeed;
    private Double hsrPlus;
    private Double stopAndGo;
    private Double acceleration;
    private List<Distance5MinDTO> distance5Min;
    private TrackerDataDTO trackerData;

    /**
     * Interní identifikace
     */
    private MatchDTO match;

    /**
     * Interní identifikace
     */
    private PlayerDTO player;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocationDTO {
        private String type;
        private List<Double> coordinates;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrackerDataDTO {
        private Long trackerMac;
        private String trackerName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Distance5MinDTO {
        private String index;
        private DistanceSliceDTO distance;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DistanceSliceDTO {
        private Integer low;
        private Integer normal;
        private Integer high;
    }
}