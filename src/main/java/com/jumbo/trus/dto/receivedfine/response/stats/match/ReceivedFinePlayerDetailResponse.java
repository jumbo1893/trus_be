package com.jumbo.trus.dto.receivedfine.response.stats.match;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReceivedFinePlayerDetailResponse {

    private List<MatchWithFinesDTO> matches;

    private List<FineWithMatchesDTO> fines;
}