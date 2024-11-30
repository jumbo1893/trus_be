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
public class TeamDTO {

    private Long id;

    private String name;

    private Long currentLeagueId;

    private List<@Positive Long> tableTeamIdList;

    private List<FootballPlayerDTO> footballPlayerList;

    private String uri;

    public TeamDTO(String name, Long currentLeagueId, String uri) {
        this.name = name;
        this.currentLeagueId = currentLeagueId;
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamDTO teamDTO = (TeamDTO) o;
        return Objects.equals(name, teamDTO.name) && Objects.equals(currentLeagueId, teamDTO.currentLeagueId) && Objects.equals(uri, teamDTO.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, currentLeagueId, uri);
    }
}
