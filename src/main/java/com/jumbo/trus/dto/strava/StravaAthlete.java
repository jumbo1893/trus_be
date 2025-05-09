package com.jumbo.trus.dto.strava;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StravaAthlete {
    private String id;
    private String username;
    private String firstname;
    private String lastname;
}