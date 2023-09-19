package com.jumbo.trus.dto.receivedfine.response.get.detailed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineDetailedDTO {

    private long id;

    private int fineNumber;

    private int fineAmount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FineDTO fine;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PlayerDTO player;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    public void addFineNumber(int number) {
        fineNumber+=number;
    }

    public void addFineAmount(int amount) {
        fineAmount+=amount;
    }

    public int returnFineAmount() {
        return fineNumber*fine.getAmount();
    }
}
