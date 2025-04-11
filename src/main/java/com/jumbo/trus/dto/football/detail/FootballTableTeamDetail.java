package com.jumbo.trus.dto.football.detail;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.TableTeamDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FootballTableTeamDetail {

    @NotNull
    private TableTeamDTO tableTeam;

    @NotNull
    private List<FootballMatchDTO> mutualMatches;

    private String aggregateScore;

    private String aggregateMatches;

    private Integer averageBirthYear;

    private BestScorer bestScorer;

    @NotNull
    private List<FootballMatchDTO> nextMatches;

    @NotNull
    private List<FootballMatchDTO> pastMatches;


}
