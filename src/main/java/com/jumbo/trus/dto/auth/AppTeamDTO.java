package com.jumbo.trus.dto.auth;

import com.jumbo.trus.dto.football.TeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppTeamDTO {

    private long id;

    private String name;

    private Long ownerId;

    private TeamDTO team;

}
