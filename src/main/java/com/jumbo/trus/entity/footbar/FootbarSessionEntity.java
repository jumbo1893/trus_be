package com.jumbo.trus.entity.footbar;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(
        name = "footbar_session",
        indexes = {
                @Index(name = "idx_footbar_session_id", columnList = "footbar_session_id")
        }
)
public class FootbarSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID session z Footbar API
     */
    @Column(name = "footbar_session_id", nullable = false, unique = true)
    private Long footbarSessionId;
    private Date startDate;
    private Date stopDate;
    private Double playingTime;
    private String title;

    /**
     * Geo location
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object location;

    private String matchType;
    private String position;
    private Double scoreStars;

    /**
     * Distance in meters
     */
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

    /**
     * High intensity running distance
     */
    private Double hsrPlus;
    private Double stopAndGo;
    private Double acceleration;

    /**
     * distance_5min z API
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "distance_5min", columnDefinition = "jsonb")
    private Object distance5Min;

    /**
     * tracker metadata
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tracker_data", columnDefinition = "jsonb")
    private Object trackerData;


    @ManyToOne(fetch = FetchType.LAZY)
    private MatchEntity match;


    @ManyToOne(fetch = FetchType.LAZY)
    private PlayerEntity player;

    @ManyToOne
    private FootbarAccountEntity footbarAccount;

    private Date syncedAt;
}