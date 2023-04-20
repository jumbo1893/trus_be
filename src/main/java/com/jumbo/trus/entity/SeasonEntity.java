package com.jumbo.trus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Entity(name = "season")
@Data
public class SeasonEntity {

    @Id
    @GeneratedValue(generator="season_seq")
    @SequenceGenerator(name = "season_seq", sequenceName = "season_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;


    @Column(nullable = false)
    private Date fromDate;

    @Column(nullable = false)
    private Date toDate;

    @OneToMany(mappedBy = "season")
    private List<MatchEntity> matchList;
}
