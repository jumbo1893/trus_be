package com.jumbo.trus.dto.receivedfine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.FineDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;

    private int fineNumber;

    @NotNull
    private FineDTO fine;

    @NotNull
    private Long playerId;

    @NotNull
    private Long matchId;

    public void addFinesToFineNumber(int oldFines) {
        fineNumber+=oldFines;
    }

    public ReceivedFineDTO(int fineNumber, @NotNull FineDTO fine, @NotNull Long playerId, @NotNull Long matchId) {
        this.fineNumber = fineNumber;
        this.fine = fine;
        this.playerId = playerId;
        this.matchId = matchId;
    }
}
