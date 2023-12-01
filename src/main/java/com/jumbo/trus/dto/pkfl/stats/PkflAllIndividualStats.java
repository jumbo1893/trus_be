package com.jumbo.trus.dto.pkfl.stats;

import com.jumbo.trus.dto.pkfl.PkflPlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PkflAllIndividualStats {

    private PkflPlayerDTO player;

    private int matches;

    private int goals;

    private int receivedGoals;

    private int ownGoals;

    private int goalkeepingMinutes;

    private int yellowCards;

    private int redCards;

    private int bestPlayer;

    private int hattrick;

    private int cleanSheet;

    private List<PkflCardComment> yellowCardComments;

    private List<PkflCardComment> redCardComments;

    private int matchPoints;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PkflAllIndividualStats that = (PkflAllIndividualStats) o;

        return Objects.equals(player, that.player);
    }

    @Override
    public int hashCode() {
        return player != null ? player.hashCode() : 0;
    }
}
