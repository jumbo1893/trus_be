package com.jumbo.trus.dto.football;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeagueDTO {

    private Long id;

    private String name;

    private int rank;

    private Organization organization;

    private String organizationUnit;

    private String uri;

    private String year;

    private List<@Positive Long> tableTeamIdList;

    private boolean currentLeague;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeagueDTO leagueDTO = (LeagueDTO) o;
        return rank == leagueDTO.rank && currentLeague == leagueDTO.currentLeague && Objects.equals(name, leagueDTO.name) && organization == leagueDTO.organization && Objects.equals(organizationUnit, leagueDTO.organizationUnit) && Objects.equals(uri, leagueDTO.uri) && Objects.equals(year, leagueDTO.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rank, organization, organizationUnit, uri, year, currentLeague);
    }
}
