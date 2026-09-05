package com.jumbo.trus.dto.ai;

import java.util.Date;

public interface AiFineSummaryProjection {

    Long getFineId();

    String getFineCode();

    String getFineName();

    Long getSeasonId();

    String getSeasonName();

    Date getSeasonFrom();

    Date getSeasonTo();

    Long getFineCount();

    Long getTotalAmount();
}
