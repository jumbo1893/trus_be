package com.jumbo.trus.dto.auth.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.auth.AppTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamWithAppTeams {

    private Long id;

    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AppTeamDTO> appTeamList;

    public TeamWithAppTeams(TeamDTO teamDTO) {
        this.id = teamDTO.getId();
        this.name = teamDTO.getName();
        this.appTeamList = new ArrayList<>();
    }
}
