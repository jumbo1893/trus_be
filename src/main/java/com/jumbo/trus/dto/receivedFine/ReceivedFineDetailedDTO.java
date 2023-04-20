package com.jumbo.trus.dto.receivedFine;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.MatchDTO;
import com.jumbo.trus.dto.PlayerDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineDetailedDTO {

    @NotNull
    @JsonProperty("_id")
    private long id;

    @NotNull
    private int fineNumber;

    @NotNull
    private FineDTO fine;

    @NotNull
    private PlayerDTO player;

    @NotNull
    private MatchDTO match;
}
