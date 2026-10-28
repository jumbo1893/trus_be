package com.jumbo.trus.dto.goal.response.get;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.service.helper.DetailedDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalDetailedDTO {

    private long id;

    private int goalNumber;

    private int assistNumber;

    private int yellowCards;

    private int redCards;

    private int goalkeepingMinutes;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PlayerDTO player;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    public GoalDetailedDTO(long id, int goalNumber, int assistNumber, PlayerDTO player, MatchDTO match) {
        this.id = id;
        this.goalNumber = goalNumber;
        this.assistNumber = assistNumber;
        this.player = player;
        this.match = match;
    }

    public void addGoals(int goals) {
        goalNumber+=goals;
    }

    public void addAssists(int assists) {
        assistNumber+=assists;
    }

    public GoalDetailedDTO(DetailedDTO detailedDTO) {
        this.id = detailedDTO.getId();
        this.goalNumber = detailedDTO.getNumber1();
        this.assistNumber = detailedDTO.getNumber2();
        this.player = detailedDTO.getPlayer();
        this.match = detailedDTO.getMatch();
    }
}
