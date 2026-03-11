package com.jumbo.trus.dto.footbar.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FootbarSessionRawDTO {

    /**
     * Footbar API session id
     */
    @JsonAlias("id")
    private Long footbarSessionId;

    @JsonAlias("start_date")
    private String startDate;

    @JsonAlias("stop_date")
    private String stopDate;

    /**
     * Effective playing time in seconds
     */
    @JsonAlias("playing_time")
    private Double playingTime;

    private String title;

    /**
     * OpenStreetMap location
     */
    private LocationDTO location;

    /**
     * 11 = Game, ss = Small-sided game, tr = Training, ru = Running
     */
    @JsonAlias("match_type")
    private String matchType;

    private String position;

    /**
     * Score 0-5
     */
    @JsonAlias("score_stars")
    private Double scoreStars;

    /**
     * Distance in meters
     */
    private Double distance;

    @JsonAlias("pass_count")
    private Integer passCount;

    @JsonAlias("shot_count")
    private Integer shotCount;

    /**
     * Max shot speed in m/s
     */
    @JsonAlias("shot_speed")
    private Double shotSpeed;

    /**
     * Average shot speed in m/s
     */
    @JsonAlias("avg_shot_speed")
    private Double avgShotSpeed;

    /**
     * Deprecated in Footbar app, but may still be present in API
     */
    @JsonAlias("dribble_count")
    private Integer dribbleCount;

    /**
     * Time with ball in seconds
     */
    @JsonAlias("time_with_ball")
    private Double timeWithBall;

    /**
     * Activity ratio (0-1)
     */
    private Double activity;

    /**
     * Activity * playing time, in seconds
     */
    @JsonAlias("time_running")
    private Double timeRunning;

    @JsonAlias("run_count")
    private Double runCount;

    @JsonAlias("sprint_count")
    private Integer sprintCount;

    /**
     * Average sprint speed in m/s
     */
    @JsonAlias("avg_sprint_speed")
    private Double avgSprintSpeed;

    /**
     * Max sprint speed in m/s
     */
    @JsonAlias("sprint_speed")
    private Double sprintSpeed;

    /**
     * High-Speed Running Plus distance (high + sprint), in meters
     */
    @JsonAlias("hsr_plus")
    private Double hsrPlus;

    /**
     * Average number of quick rhythm changes by 5 minutes
     */
    @JsonAlias("stop_and_go")
    private Double stopAndGo;

    /**
     * Standardized time to reach 18 km/h, in seconds
     */
    private Double acceleration;

    /**
     * Distance split by 5-minute segments
     */
    @JsonAlias("distance_5min")
    private List<Distance5MinDTO> distance5Min;

    /**
     * Tracker metadata
     */
    @JsonAlias("tracker_data")
    private TrackerDataDTO trackerData;

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
        @JsonAlias("tracker_mac")
        private Long trackerMac;

        @JsonAlias("tracker_name")
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
        /**
         * Walking distance in meters
         */
        private Integer low;

        /**
         * Running distance in meters
         */
        private Integer normal;

        /**
         * Sprint distance in meters
         */
        private Integer high;
    }
}