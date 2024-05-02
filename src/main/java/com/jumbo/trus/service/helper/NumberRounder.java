package com.jumbo.trus.service.helper;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor
public class NumberRounder {

    public float roundFloat(int places, float number) {
        return roundNumber(places, BigDecimal.valueOf(number)).floatValue();
    }

    public double roundDouble(int places, double number) {
        return roundNumber(places, BigDecimal.valueOf(number)).doubleValue();
    }

    private BigDecimal roundNumber(int places, BigDecimal bd) {
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd;
    }

    public String roundDoubleToString(int places, double number) {
        double shortNumber = roundDouble(places, number);
        if (number % 1 == 0) {
            return String.valueOf(Math.round(number));
        }
        return String.valueOf(shortNumber);
    }


    public String roundFloatToString(int places, float number) {
        float shortNumber = roundFloat(places, number);
        if (number % 1 == 0) {
            return String.valueOf(Math.round(number));
        }
        return String.valueOf(shortNumber);
    }


}
