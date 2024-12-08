package com.jumbo.trus.entity.football.detail;

import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.entity.football.LeagueEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Entity
@Data
@Immutable // View je neměnné, Hibernate se nesnaží provádět INSERT, UPDATE, DELETE
@Subselect("SELECT * FROM best_scorer_view") // Hibernate nepoužije @Table, ale použije SQL SELECT
public class BestScorerView {

    @EmbeddedId
    private BestScorerId id;

    @ManyToOne
    @MapsId("teamId") // mapování teamId z BestScorerId na team_id
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    private TeamEntity team;

    @ManyToOne
    @MapsId("playerId") // mapování playerId z BestScorerId na player_id
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private FootballPlayerEntity player;

    @ManyToOne
    @MapsId("leagueId") // mapování leagueId z BestScorerId na league_id
    @JoinColumn(name = "league_id", insertable = false, updatable = false)
    private LeagueEntity league;

    private Integer totalGoals;
}
