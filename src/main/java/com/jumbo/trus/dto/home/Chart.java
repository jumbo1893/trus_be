package com.jumbo.trus.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chart {

    private int fineMaximum;

    private int beerMaximum;

    private List<Integer> beerLabels;

    private List<Integer> fineLabels;

    private List<Coordinate> coordinates;




}
