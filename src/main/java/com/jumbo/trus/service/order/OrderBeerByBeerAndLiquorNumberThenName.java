package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.beer.multi.BeerNoMatchWithPlayerDTO;

import java.util.Comparator;

public class OrderBeerByBeerAndLiquorNumberThenName implements Comparator<BeerNoMatchWithPlayerDTO> {

    public int compare(BeerNoMatchWithPlayerDTO o1, BeerNoMatchWithPlayerDTO o2) {
        int total1 = o1.getBeerNumber() + o1.getLiquorNumber();
        int total2 = o2.getBeerNumber() + o2.getLiquorNumber();

        if (total1 != total2) {
            return Integer.compare(total2, total1);
        }
        return o1.getPlayer().getName().compareTo(o2.getPlayer().getName());
    }
}
