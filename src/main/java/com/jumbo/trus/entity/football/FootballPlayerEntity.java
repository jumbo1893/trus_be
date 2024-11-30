package com.jumbo.trus.entity.football;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity(name = "football_player")
@Data
public class FootballPlayerEntity {

    @Id
    @GeneratedValue(generator = "football_player_seq")
    @SequenceGenerator(name = "football_player_seq", sequenceName = "football_player_seq", allocationSize = 1)
    private Long id;

    private String name;

    private int birthYear;

    @ManyToMany(mappedBy = "footballPlayerList")
    private List<TeamEntity> teamList;

    private String email;

    private String phoneNumber;

    private String uri;

}
