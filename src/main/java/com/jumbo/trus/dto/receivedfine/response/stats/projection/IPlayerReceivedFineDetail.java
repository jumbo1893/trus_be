package com.jumbo.trus.dto.receivedfine.response.stats.projection;

import java.util.Date;

public interface IPlayerReceivedFineDetail {

    Long getMatchId();

    String getMatchName();

    Date getMatchDate();

    Long getSeasonId();

    Long getFineId();

    String getFineName();

    Integer getFineAmount();

    Long getFineCount();

    Long getTotalAmount();
}