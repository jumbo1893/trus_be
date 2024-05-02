package com.jumbo.trus.service.pkfl.fact.helper;

import com.jumbo.trus.service.helper.NumberRounder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoubleAndString {

    private double total;
    private String text;

    public double getTotalRounded(int places) {
        NumberRounder numberRounder = new NumberRounder();
        return numberRounder.roundDouble(places, total);
    }

    public String getTotalRoundedInString(int places) {
        NumberRounder numberRounder = new NumberRounder();
        return numberRounder.roundDoubleToString(places, total);
    }


}
