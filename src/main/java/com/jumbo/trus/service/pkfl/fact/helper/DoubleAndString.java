package com.jumbo.trus.service.pkfl.fact.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoubleAndString {

    private double total;
    private String text;

    public double getTotalRounded(int places) {
        BigDecimal bd = BigDecimal.valueOf(total);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


}
