package com.jumbo.trus.dto.match;

import com.jumbo.trus.service.helper.DateFormatter;

public class MatchHelper {

    final MatchDTO matchDTO;

    public MatchHelper(MatchDTO matchDTO) {
        this.matchDTO = matchDTO;
    }

    public String getMatchWithOpponentName() {
        return matchDTO.isHome() ? "Liščí Trus - " + matchDTO.getName() : matchDTO.getName() + " - Liščí Trus";
    }

    public String getMatchWithOpponentNameAndDate() {
        return getMatchWithOpponentName() + ", " + DateFormatter.formatToMatchDate(matchDTO);
    }
}
