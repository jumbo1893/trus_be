package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.SeasonDTO;

import java.util.Comparator;

public class OrderSeasonByDate implements Comparator<SeasonDTO> {

    public int compare(SeasonDTO o1, SeasonDTO o2) {
        return o2.getFromDate().compareTo(o1.getFromDate());
    }
}
