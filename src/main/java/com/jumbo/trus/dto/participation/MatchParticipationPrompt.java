package com.jumbo.trus.dto.participation;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchParticipationPrompt {

    private FootballMatchDTO footballMatch;
    private PlayerDTO currentPlayer;
    private MatchParticipationStatus currentStatus;
    private boolean reconsideration;
    private List<PlayerDTO> eligiblePlayers = new ArrayList<>();
}
