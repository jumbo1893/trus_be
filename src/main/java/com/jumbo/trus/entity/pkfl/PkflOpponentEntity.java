package com.jumbo.trus.entity.pkfl;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity(name = "pkfl_opponent")
@Data
public class PkflOpponentEntity {

    @Id
    @GeneratedValue(generator = "pkfl_opponent_seq")
    @SequenceGenerator(name = "pkfl_opponent_seq", sequenceName = "pkfl_opponent_seq", allocationSize = 1)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "opponent")
    private List<PkflMatchEntity> matchList;

}
