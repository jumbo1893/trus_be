package com.jumbo.trus.dto.strava;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StravaTokenResponse {

    @JsonAlias("access_token")
    private String accessToken;

    @JsonAlias("refresh_token")
    private String refreshToken;

    @JsonAlias("expires_at")
    private Long expiresAt;

    private StravaAthlete athlete;
}