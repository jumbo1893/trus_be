package com.jumbo.trus.entity.strava;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "strava_athlete")
@Data
public class AthleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private UserEntity user;

    private String stravaAthleteId;
    private String accessToken;
    private String refreshToken;
    private Long tokenExpiry;
}
