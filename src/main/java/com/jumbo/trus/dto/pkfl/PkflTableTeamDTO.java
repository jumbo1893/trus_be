package com.jumbo.trus.dto.pkfl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PkflTableTeamDTO {

    private PkflOpponentDTO opponent;

    private int rank;

    private int matches;

    private int wins;

    private int draws;

    private int losses;

    private int goalsScored;

    private int goalsReceived;

    private String penalty;

    private int points;

    private Long pkflMatchId;
}
