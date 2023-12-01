package com.jumbo.trus.entity.pkfl;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity(name = "pkfl_player")
@Data
public class PkflPlayerEntity {

    @Id
    @GeneratedValue(generator = "pkfl_player_seq")
    @SequenceGenerator(name = "pkfl_player_seq", sequenceName = "pkfl_player_seq", allocationSize = 1)
    private Long id;

    private String name;

    @OneToMany
    private List<PkflIndividualStatsEntity> individualStatsList;

}
