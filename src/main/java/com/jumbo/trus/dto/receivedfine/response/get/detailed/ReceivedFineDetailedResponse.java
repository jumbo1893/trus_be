package com.jumbo.trus.dto.receivedfine.response.get.detailed;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineDetailedResponse {

    private int playersCount = 0;

    private int matchesCount = 0;

    private int finesNumber = 0;

    private int finesAmount = 0;

    @NotNull
    private List<ReceivedFineDetailedDTO> fineList;

    public void addFines(int fines) {
        finesNumber+=fines;
    }

    public void addFineAmount(int fineAmount) {
        finesAmount+=fineAmount;
    }



}
