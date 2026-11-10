package com.jumbo.trus.dto.home;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.dto.home.stats.StatsBoardData;
import com.jumbo.trus.dto.participation.MatchParticipationPrompt;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeSetup {

    @NotNull
    private String nextBirthday;

    private List<String> randomFacts;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FootballMatchDetail> nextAndLastFootballMatch;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Chart chart;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Chart> charts;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DashboardMatch nextMatch;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DashboardMatch lastMatch;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchParticipationPrompt participationPrompt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchParticipationStatus currentParticipationStatus;

    private Integer attendingPlayerCount;

    private List<StatsBoardData> statsBoards;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String footbarWarning;

    private List<PendingTeamMemberDTO> pendingTeamMembers;

}
