package com.jumbo.trus.entity.pkfl;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity(name = "pkfl_referee")
@Data
public class PkflRefereeEntity {

    @Id
    @GeneratedValue(generator = "pkfl_referee_seq")
    @SequenceGenerator(name = "pkfl_referee_seq", sequenceName = "pkfl_referee_seq", allocationSize = 1)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "referee")
    private List<PkflMatchEntity> matchList;

}
