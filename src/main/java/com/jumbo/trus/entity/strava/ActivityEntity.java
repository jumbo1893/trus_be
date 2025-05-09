package com.jumbo.trus.entity.strava;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity(name = "strava_activity")
@Data
public class ActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "athlete_id")
    private AthleteEntity athlete;
    private String name;
    private String stravaActivityId;
    private Float distanceKm;
    private Integer durationSeconds;
    private Date startTime;
    private Date endTime;
    private Integer movingTimeSeconds;
    private String type;
    private Float averageSpeed;
    private Float maxSpeed;
    private Float calories;
    private Float averageHeartRate;
    private Float maxHeartRate;
}
