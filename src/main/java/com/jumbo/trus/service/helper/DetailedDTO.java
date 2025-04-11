package com.jumbo.trus.service.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailedDTO {

    private long id;

    private int number1;

    private int number2;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PlayerDTO player;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    public void addNumber1(int number) {
        number1+=number;
    }

    public void addNumber2(int number) {
        number2+=number;
    }

    public DetailedDTO(GoalDetailedDTO goalDetailedDTO) {
        this.id = goalDetailedDTO.getId();
        this.number1 = goalDetailedDTO.getGoalNumber();
        this.number2 = goalDetailedDTO.getAssistNumber();
        this.player = goalDetailedDTO.getPlayer();
        this.match = goalDetailedDTO.getMatch();
    }

    public DetailedDTO(BeerDetailedDTO beerDetailedDTO) {
        this.id = beerDetailedDTO.getId();
        this.number1 = beerDetailedDTO.getBeerNumber();
        this.number2 = beerDetailedDTO.getLiquorNumber();
        this.player = beerDetailedDTO.getPlayer();
        this.match = beerDetailedDTO.getMatch();
    }
}
