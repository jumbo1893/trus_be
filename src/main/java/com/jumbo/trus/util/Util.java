package com.jumbo.trus.util;

public class Util {

    public float getAverage(Integer sum, Integer count) {
        return (sum == null || count == null || count == 0) ? 0 : (float) sum / count;
    }
}
