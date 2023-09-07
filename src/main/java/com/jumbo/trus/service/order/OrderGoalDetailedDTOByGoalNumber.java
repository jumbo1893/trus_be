package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedDTO;

import java.util.Comparator;

public class OrderGoalDetailedDTOByGoalNumber implements Comparator<GoalDetailedDTO> {

    public int compare(GoalDetailedDTO o1, GoalDetailedDTO o2) {
        int total1 = o1.getGoalNumber() + o1.getAssistNumber();
        int total2 = o2.getGoalNumber() + o2.getAssistNumber();

        if (total1 != total2) {
            return Integer.compare(total2, total1);
        }
        if (o1.getGoalNumber() != o2.getGoalNumber()) {
            return Integer.compare(o2.getGoalNumber(), o1.getGoalNumber());
        }
        return Integer.compare(o2.getAssistNumber(), o1.getAssistNumber());
    }
}
