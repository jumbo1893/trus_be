package com.jumbo.trus.dto.ai;

import java.util.Date;

public interface AiRepeatOpponentProjection {

    String getOpponent();

    Long getCurrentSeasonMatchCount();

    Date getFirstCurrentSeasonMatch();

    Long getHistoricalMatchCount();

    Date getLastHistoricalMatch();
}
