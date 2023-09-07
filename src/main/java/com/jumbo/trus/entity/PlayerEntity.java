package com.jumbo.trus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity(name = "player")
@Data
public class PlayerEntity {

    @Id
    @GeneratedValue(generator="player_seq")
    @SequenceGenerator(name = "player_seq", sequenceName = "player_seq", allocationSize = 1)
    private long id;

    @Column(nullable = false)
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @Column(nullable = false)
    private Date birthday;

    @Column(nullable = false)
    private boolean fan;

    @Column(nullable = false)
    private boolean active;

    @ManyToMany(mappedBy = "playerList")
    private List<MatchEntity> matchList;

    @OneToMany(mappedBy = "player")
    private List<BeerEntity> beerList;

    @OneToMany(mappedBy = "player")
    private List<ReceivedFineEntity> fineList;

    @OneToMany(mappedBy = "player")
    private List<GoalEntity> goalList;
}
