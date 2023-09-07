package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;

import java.util.Comparator;

public class OrderBeerDetailedDTOByBeerAndLiquorNumber implements Comparator<BeerDetailedDTO> {

    public int compare(BeerDetailedDTO o1, BeerDetailedDTO o2) {
        int total1 = o1.getBeerNumber() + o1.getLiquorNumber();
        int total2 = o2.getBeerNumber() + o2.getLiquorNumber();

        if (total1 != total2) {
            return Integer.compare(total2, total1);
        }
        if (o1.getBeerNumber() != o2.getBeerNumber()) {
            return Integer.compare(o2.getBeerNumber(), o1.getBeerNumber());
        }
        return Integer.compare(o2.getLiquorNumber(), o1.getLiquorNumber());
    }
}
