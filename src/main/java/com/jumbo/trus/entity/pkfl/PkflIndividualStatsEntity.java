package com.jumbo.trus.entity.pkfl;

import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "pkfl_individual_stats")
@Data
public class PkflIndividualStatsEntity {

    @Id
    @GeneratedValue(generator = "pkfl_individual_stats_seq")
    @SequenceGenerator(name = "pkfl_individual_stats_seq", sequenceName = "pkfl_individual_stats_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private PkflPlayerEntity player;

    private int goals;

    private int receivedGoals;

    private int ownGoals;

    private int goalkeepingMinutes;

    private int yellowCards;

    private int redCards;

    private boolean bestPlayer;

    private boolean cleanSheet;

    private boolean hattrick;

    private String yellowCardComment;

    private String redCardComment;

    @ManyToOne
    private PkflMatchEntity match;


}
