package com.jumbo.trus.dto.strava;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;


@Data
public class StravaActivity {

    private Long id;

    private String stravaActivityId;

    private String name;

    private Float distance;

    private String type;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date endTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("start_date")
    private String startDate;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("end_date")
    private String endDate;

    @JsonAlias("elapsed_time")
    private Integer elapsedTime;

    @JsonAlias("moving_time")
    private Integer movingTime;

    @JsonAlias("average_speed")
    private Float averageSpeed;

    @JsonAlias("max_speed")
    private Float maxSpeed;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Float calories;

    @JsonAlias("average_heartrate")
    private Float averageHeartRate;

    @JsonAlias("max_heartrate")
    private Float maxHeartRate;
}