package com.jumbo.trus.dto.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedirectDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Redirect redirect;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SeasonDTO season;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PlayerDTO player;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FootballMatchDTO footballMatch;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MatchDTO match;
}
