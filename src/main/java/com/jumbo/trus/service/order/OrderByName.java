package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.match.MatchDTO;

import java.util.Comparator;

public class OrderByName implements Comparator<MatchDTO> {
    final boolean descending;

        public OrderByName(boolean descending) {
            this.descending = descending;
        }
    public int compare(MatchDTO a, MatchDTO b) {
        if (descending) {
            return b.getDate().compareTo(a.getDate());
        }
        return a.getDate().compareTo(b.getDate());
    }
}
