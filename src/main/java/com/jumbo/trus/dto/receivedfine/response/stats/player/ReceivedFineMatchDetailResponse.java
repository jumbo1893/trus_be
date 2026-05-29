package com.jumbo.trus.dto.receivedfine.response.stats.player;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReceivedFineMatchDetailResponse {

    private List<PlayerWithFinesDTO> players;

    private List<FineWithPlayersDTO> fines;
}