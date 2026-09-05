package com.jumbo.trus.entity.filter;


import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StatisticsFilter extends BaseSeasonFilter {


    private Boolean matchStatsOrPlayerStats;

    private Boolean detailed;

    private String stringFilter;

    private Boolean splitPlayerFinesByFine;

    private List<Long> seasonIds = new ArrayList<>();
    private List<Long> playerIds = new ArrayList<>();
    private List<Long> fineIds = new ArrayList<>();
    private List<String> opponentNames = new ArrayList<>();

    public StatisticsFilter(Long playerId, Long matchId, Long seasonId, Boolean matchStatsOrPlayerStats) {
        super(playerId, matchId, seasonId);
        this.matchStatsOrPlayerStats = matchStatsOrPlayerStats;
    }

    public StatisticsFilter(Long playerId, Long matchId, Long seasonId, Boolean matchStatsOrPlayerStats, AppTeamEntity appTeam) {
        super(playerId, matchId, seasonId, appTeam);
        this.matchStatsOrPlayerStats = matchStatsOrPlayerStats;
    }


}
