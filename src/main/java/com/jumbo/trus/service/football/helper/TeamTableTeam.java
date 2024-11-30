package com.jumbo.trus.service.football.helper;

import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamTableTeam {

    private TeamDTO team;

    private TableTeamDTO tableTeam;
}
