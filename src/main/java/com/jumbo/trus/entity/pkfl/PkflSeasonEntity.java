package com.jumbo.trus.entity.pkfl;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity(name = "pkfl_season")
@Data
public class PkflSeasonEntity {

    @Id
    @GeneratedValue(generator = "pkfl_season_seq")
    @SequenceGenerator(name = "pkfl_season_seq", sequenceName = "pkfl_season_seq", allocationSize = 1)
    private Long id;

    private String url;

    private String name;

    @OneToMany(mappedBy = "season")
    private List<PkflMatchEntity> matchList;
}
