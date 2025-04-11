package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "received_fine")
@Data
public class ReceivedFineEntity {

    @Id
    @GeneratedValue(generator = "received_fine_seq")
    @SequenceGenerator(name = "received_fine_seq", sequenceName = "received_fine_seq", allocationSize = 1)
    private Long id;

    private int fineNumber;

    @ManyToOne
    private FineEntity fine;

    @ManyToOne
    private MatchEntity match;

    @ManyToOne
    private PlayerEntity player;

    @ManyToOne
    private AppTeamEntity appTeam;
}
