package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;

import java.util.Comparator;

public class OrderReceivedFineDetailedDTOByFineAmount implements Comparator<ReceivedFineDetailedDTO> {

    public int compare(ReceivedFineDetailedDTO o1, ReceivedFineDetailedDTO o2) {

        if (o1.getFineAmount() != o2.getFineAmount()) {
            return Integer.compare(o2.getFineAmount(), o1.getFineAmount());
        }
        return Integer.compare(o2.getFineNumber(), o1.getFineNumber());
    }
}
