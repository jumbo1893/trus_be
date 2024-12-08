package com.jumbo.trus.entity.football.detail;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.*;
import lombok.Data;

@Embeddable
@Data
public class BestScorerId implements Serializable {

    private Long teamId;
    private Long playerId;
    private Long leagueId;

    public BestScorerId() {}

    public BestScorerId(Long teamId, Long playerId, Long leagueId) {
        this.teamId = teamId;
        this.playerId = playerId;
        this.leagueId = leagueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BestScorerId that = (BestScorerId) o;
        return Objects.equals(teamId, that.teamId) &&
                Objects.equals(playerId, that.playerId) &&
                Objects.equals(leagueId, that.leagueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, playerId, leagueId);
    }
}
