package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.match.MatchDTO;

import java.util.Comparator;

public class OrderMatchByDate implements Comparator<MatchDTO> {

    public int compare(MatchDTO o1, MatchDTO o2) {
        return o2.getDate().compareTo(o1.getDate());
    }
}
