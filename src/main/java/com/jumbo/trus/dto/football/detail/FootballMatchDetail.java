package com.jumbo.trus.dto.football.detail;

import com.jumbo.trus.dto.football.FootballMatchDTO;
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
public class FootballMatchDetail {

    @NotNull
    private FootballMatchDTO footballMatchDTO;

    @NotNull
    private List<FootballMatchDTO> mutualMatches;

    private String aggregateScore;

    private String aggregateMatches;

    private Integer homeTeamAverageBirthYear;

    private Integer awayTeamAverageBirthYear;

    private BestScorer homeTeamBestScorer;

    private BestScorer awayTeamBestScorer;


}
