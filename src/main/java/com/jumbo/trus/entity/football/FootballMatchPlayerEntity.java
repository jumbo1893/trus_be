package com.jumbo.trus.entity.football;

import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "football_match_player")
@Data
public class FootballMatchPlayerEntity {

    @Id
    @GeneratedValue(generator = "football_match_player_seq")
    @SequenceGenerator(name = "football_match_player_seq", sequenceName = "football_match_player_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private FootballPlayerEntity player;

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
    private FootballMatchEntity match;

    @ManyToOne
    private TeamEntity team;


}
