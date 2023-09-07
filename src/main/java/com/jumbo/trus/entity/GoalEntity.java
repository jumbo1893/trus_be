package com.jumbo.trus.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity(name = "goal")
@Data
public class GoalEntity {

    @Id
    @GeneratedValue(generator="goal_seq")
    @SequenceGenerator(name = "goal_seq", sequenceName = "goal_seq", allocationSize = 1)
    private Long id;

    private int goalNumber;

    private int assistNumber;

    @ManyToOne
    private MatchEntity match;

    @ManyToOne
    private PlayerEntity player;
}
