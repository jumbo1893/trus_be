package com.jumbo.trus.entity.filter;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class StepFilter {

    private Long userId;

    //defaultní hodnota
    private int limit = 1000;

    public StepFilter(Long userId) {
        this.userId = userId;
    }
}
