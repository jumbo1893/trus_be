package com.jumbo.trus.dto.receivedfine.response.get.setup;

import com.jumbo.trus.dto.FineDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineSetupForPlayerResponse {


    private long id;

    private int fineNumber;

    @NotNull
    private FineDTO fine;
}
