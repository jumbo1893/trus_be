package com.jumbo.trus.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Coordinate {

    private String matchInitials;

    private int beerNumber;

    private int liquorNumber;

    private int fineAmount;

}
