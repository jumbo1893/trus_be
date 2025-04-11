package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import jakarta.persistence.*;
import lombok.Data;


@Entity(name = "beer")
@Data
public class BeerEntity {

    @Id
    @GeneratedValue(generator = "beer_seq")
    @SequenceGenerator(name = "beer_seq", sequenceName = "beer_seq", allocationSize = 1)
    private Long id;

    private int beerNumber;

    private int liquorNumber;

    @ManyToOne
    private MatchEntity match;

    @ManyToOne
    private PlayerEntity player;

    @ManyToOne
    private AppTeamEntity appTeam;
}
