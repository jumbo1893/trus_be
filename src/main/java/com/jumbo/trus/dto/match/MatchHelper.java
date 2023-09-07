package com.jumbo.trus.dto.match;

import java.text.SimpleDateFormat;

public class MatchHelper {

    final MatchDTO matchDTO;

    public MatchHelper(MatchDTO matchDTO) {
        this.matchDTO = matchDTO;
    }

    public String getMatchWithOpponentName() {
        return matchDTO.isHome() ? "Liščí Trus - " + matchDTO.getName() : matchDTO.getName() + " - Liščí Trus";
    }

    public String getMatchWithOpponentNameAndDate() {
        return getMatchWithOpponentName() + ", " + matchDateToString();
    }

    public String matchDateToString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MM. yyyy");
        return dateFormat.format(matchDTO.getDate());
    }
}
