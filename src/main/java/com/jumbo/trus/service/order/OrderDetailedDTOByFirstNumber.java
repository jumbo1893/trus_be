package com.jumbo.trus.service.order;

import com.jumbo.trus.service.helper.DetailedDTO;

import java.util.Comparator;

public class OrderDetailedDTOByFirstNumber implements Comparator<DetailedDTO> {

    public int compare(DetailedDTO o1, DetailedDTO o2) {
        int total1 = o1.getNumber1() + o1.getNumber2();
        int total2 = o2.getNumber1() + o2.getNumber2();

        if (total1 != total2) {
            return Integer.compare(total2, total1);
        }
        if (o1.getNumber1() != o2.getNumber2()) {
            return Integer.compare(o2.getNumber1(), o1.getNumber1());
        }
        return Integer.compare(o2.getNumber2(), o1.getNumber2());
    }
}
