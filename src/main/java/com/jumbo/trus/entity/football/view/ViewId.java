package com.jumbo.trus.entity.football.view;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
public class ViewId implements Serializable {

    private Long teamId;
    private Long playerId;
    private Long leagueId;

    public ViewId() {}

    public ViewId(Long teamId, Long playerId, Long leagueId) {
        this.teamId = teamId;
        this.playerId = playerId;
        this.leagueId = leagueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViewId that = (ViewId) o;
        return Objects.equals(teamId, that.teamId) &&
                Objects.equals(playerId, that.playerId) &&
                Objects.equals(leagueId, that.leagueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, playerId, leagueId);
    }
}
