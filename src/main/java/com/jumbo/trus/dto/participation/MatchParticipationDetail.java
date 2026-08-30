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
public class MatchParticipationDetail {

    private FootballMatchDTO footballMatch;
    private PlayerDTO currentPlayer;
    private MatchParticipationStatus currentStatus;
    private List<MatchParticipationMemberDTO> attendingPlayers = new ArrayList<>();
    private List<MatchParticipationMemberDTO> attendingFans = new ArrayList<>();
    private List<MatchParticipationMemberDTO> maybePlayers = new ArrayList<>();
    private List<MatchParticipationMemberDTO> maybeFans = new ArrayList<>();
    private List<MatchParticipationMemberDTO> notAttendingPlayers = new ArrayList<>();
    private List<MatchParticipationMemberDTO> notAttendingFans = new ArrayList<>();
    private List<PlayerDTO> eligiblePlayers = new ArrayList<>();
}
