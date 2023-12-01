package com.jumbo.trus.dto.pkfl;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PkflMatchDetail {

    @NotNull
    private PkflMatchDTO pkflMatch;

    @NotNull
    private List<PkflMatchDTO> commonMatches;

    private String aggregateScore;

    private String aggregateMatches;

}
