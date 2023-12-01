package com.jumbo.trus.entity.pkfl;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity(name = "pkfl_stadium")
@Data
public class PkflStadiumEntity {

    @Id
    @GeneratedValue(generator = "pkfl_stadium_seq")
    @SequenceGenerator(name = "pkfl_stadium_seq", sequenceName = "pkfl_stadium_seq", allocationSize = 1)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "stadium")
    private List<PkflMatchEntity> matchList;
}
