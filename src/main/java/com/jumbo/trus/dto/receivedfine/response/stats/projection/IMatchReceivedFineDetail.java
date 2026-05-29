package com.jumbo.trus.dto.receivedfine.response.stats.projection;

public interface IMatchReceivedFineDetail {

    Long getPlayerId();

    String getPlayerName();

    Long getFineId();

    String getFineName();

    Integer getFineAmount();

    Long getFineCount();

    Long getTotalAmount();
}