package com.jumbo.trus.entity.pkfl;

import com.jumbo.trus.entity.MatchEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;


@Entity(name = "pkfl_match")
@Data
public class PkflMatchEntity {

    @Id
    @GeneratedValue(generator = "pkfl_match_seq")
    @SequenceGenerator(name = "pkfl_match_seq", sequenceName = "pkfl_match_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private PkflOpponentEntity opponent;

    private int round;

    private String league;

    private Date date;

    @ManyToOne
    private PkflStadiumEntity stadium;

    @ManyToOne
    private PkflRefereeEntity referee;

    private Integer trusGoalNumber;

    private Integer opponentGoalNumber;

    private boolean homeMatch;

    private String urlResult;

    @Column(columnDefinition = "TEXT")
    private String refereeComment;

    private boolean alreadyPlayed;

    @ManyToOne
    private PkflSeasonEntity season;

    @OneToMany(mappedBy = "match")
    private List<PkflIndividualStatsEntity> playerList;

    @OneToMany(mappedBy = "pkflMatch")
    private List<MatchEntity> matchList;
}
