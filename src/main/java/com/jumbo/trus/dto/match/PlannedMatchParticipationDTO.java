package com.jumbo.trus.dto.match;

import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import lombok.Data;
import java.util.List;

@Data
public class PlannedMatchParticipationDTO {
    private MatchParticipationStatus currentStatus;
    private Boolean currentPlaying;
    private int attendingPlayerCount;
    private int maybePlayerCount;
    private int notAttendingPlayerCount;
    private List<String> attendingPlayers;
    private List<String> maybePlayers;
    private List<String> notAttendingPlayers;
}
