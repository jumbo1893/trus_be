package com.jumbo.trus.dto.strava;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AthleteActivities {

    private Long id;
    private UserDTO user;
    private PlayerDTO player;
    private List<StravaActivity> activities;
}