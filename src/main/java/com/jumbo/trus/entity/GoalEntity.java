package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import jakarta.persistence.*;
import lombok.Data;


@Entity(name = "goal")
@Data
public class GoalEntity {

    @Id
    @GeneratedValue(generator="goal_seq")
    @SequenceGenerator(name = "goal_seq", sequenceName = "goal_seq", allocationSize = 1)
    private Long id;

    private int goalNumber;

    private int assistNumber;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int yellowCards;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int redCards;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int goalkeepingMinutes;

    @ManyToOne
    private MatchEntity match;

    @ManyToOne
    private PlayerEntity player;

    @ManyToOne
    private AppTeamEntity appTeam;
}
