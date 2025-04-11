package com.jumbo.trus.dto.auth;

import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.auth.UserTeamRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppTeamRegistration {

    private String name;

    private Long footballTeamId;

}
