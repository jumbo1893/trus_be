package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;

import java.util.Comparator;

public class OrderReceivedFineDetailedDTOByFineNumber implements Comparator<ReceivedFineDetailedDTO> {

    public int compare(ReceivedFineDetailedDTO o1, ReceivedFineDetailedDTO o2) {
        return Integer.compare(o2.getFineNumber(), o1.getFineNumber());
    }
}
