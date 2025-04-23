package com.jumbo.trus.dto.auth.registration;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.Organization;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeagueWithTeams {

    private Long id;

    private String name;

    private int rank;

    private Organization organization;

    private String organizationUnit;

    private String year;

    private List<TeamWithAppTeams> teamWithAppTeamsList;

    public LeagueWithTeams(LeagueDTO leagueDTO) {
        this.id = leagueDTO.getId();
        this.name = leagueDTO.getName();
        this.rank = leagueDTO.getRank();
        this.organization = leagueDTO.getOrganization();
        this.organizationUnit = leagueDTO.getOrganizationUnit();
        this.year = leagueDTO.getYear();
        this.teamWithAppTeamsList = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeagueWithTeams that = (LeagueWithTeams) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
