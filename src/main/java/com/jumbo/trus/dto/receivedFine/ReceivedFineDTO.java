package com.jumbo.trus.dto.receivedFine;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineDTO {

    @NotNull
    @JsonProperty("_id")
    private long id;

    @NotNull
    private int fineNumber;

    @NotNull
    private Long fineId;

    @NotNull
    private Long playerId;

    @NotNull
    private Long matchId;

    public void addFinesToFineNumber(int oldFines) {
        fineNumber+=oldFines;
    }
}
