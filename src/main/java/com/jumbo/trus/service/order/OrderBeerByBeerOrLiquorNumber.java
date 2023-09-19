package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;

import java.util.Comparator;

public class OrderBeerByBeerOrLiquorNumber implements Comparator<BeerDetailedDTO> {

    final boolean beer;

    public OrderBeerByBeerOrLiquorNumber(boolean beer) {
        this.beer = beer;
    }

    public int compare(BeerDetailedDTO o1, BeerDetailedDTO o2) {
        if (beer) {
            return Integer.compare(o2.getBeerNumber(), o1.getBeerNumber());
        }
        return Integer.compare(o2.getLiquorNumber(), o1.getLiquorNumber());
    }
}
