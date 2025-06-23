package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "dynamic_text")
@Data
public class DynamicTextEntity {

    @Id
    @GeneratedValue(generator = "dynamic_text_seq")
    @SequenceGenerator(name = "dynamic_text_seq", sequenceName = "dynamic_text_seq", allocationSize = 1)
    private Long id;

    private String name;

    private int rank;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @ManyToOne
    private AppTeamEntity appTeam;

}
