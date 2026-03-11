package com.jumbo.trus.dto.footbar.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootbarSessionSetup {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;

    @NotNull
    private SeasonDTO season;

    private List<MatchDTO> matches;
    private List<FootbarAccountSessions> sessions;
}