package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;

import java.util.Comparator;

public class OrderBeerByAttendance implements Comparator<BeerDetailedDTO> {

    final boolean desc;

    public OrderBeerByAttendance(boolean desc) {
        this.desc = desc;
    }

    public int compare(BeerDetailedDTO o1, BeerDetailedDTO o2) {
        if (desc) {
            return Integer.compare(o2.getMatch().getPlayerIdList().size(), o1.getMatch().getPlayerIdList().size());
        }
        return Integer.compare(o1.getMatch().getPlayerIdList().size(), o2.getMatch().getPlayerIdList().size());
    }
}
