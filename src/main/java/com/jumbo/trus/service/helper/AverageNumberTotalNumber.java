package com.jumbo.trus.service.helper;

import lombok.Data;

@Data
public class AverageNumberTotalNumber {
    final int totalNumber1;
    final int totalNumber2;

    public AverageNumberTotalNumber(int totalNumber1, int totalNumber2) {
        this.totalNumber1 = totalNumber1;
        this.totalNumber2 = totalNumber2;
    }

    public float getAverage() {
        return (float) totalNumber1 /totalNumber2;
    }
}
