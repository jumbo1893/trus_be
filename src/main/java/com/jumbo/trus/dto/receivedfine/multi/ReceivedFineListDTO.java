package com.jumbo.trus.dto.receivedfine.multi;

import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineListDTO {

    @NotNull
    private Long matchId;

    private List<Long> playerIdList;

    private Long playerId;

    @NotNull
    private List<ReceivedFineDTO> fineList;

}
