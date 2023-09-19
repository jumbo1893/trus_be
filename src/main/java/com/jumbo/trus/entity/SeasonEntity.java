package com.jumbo.trus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

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


    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @Column(nullable = false)
    private Date fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @Column(nullable = false)
    private Date toDate;

    @ColumnDefault("true")
    private boolean editable = true;

    @OneToMany(mappedBy = "season")
    private List<MatchEntity> matchList;
}
