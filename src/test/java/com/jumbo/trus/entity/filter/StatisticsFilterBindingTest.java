package com.jumbo.trus.entity.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestDataBinder;
import static org.assertj.core.api.Assertions.assertThat;

class StatisticsFilterBindingTest {
    @Test
    void bindsMultipleSelectionsWithoutSplittingCommasInOpponentNames() {
        StatisticsFilter filter = new StatisticsFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("seasonIds[0]", "1");
        request.addParameter("seasonIds[1]", "2");
        request.addParameter("playerIds[0]", "3");
        request.addParameter("playerIds[1]", "4");
        request.addParameter("fineIds[0]", "5");
        request.addParameter("fineIds[1]", "6");
        request.addParameter("opponentNames[0]", "TJ A, z.s.");
        request.addParameter("opponentNames[1]", "TJ B");
        ServletRequestDataBinder binder = new ServletRequestDataBinder(filter);
        binder.bind(request);
        assertThat(binder.getBindingResult().hasErrors()).isFalse();
        assertThat(filter.getSeasonIds()).containsExactly(1L, 2L);
        assertThat(filter.getPlayerIds()).containsExactly(3L, 4L);
        assertThat(filter.getFineIds()).containsExactly(5L, 6L);
        assertThat(filter.getOpponentNames()).containsExactly("TJ A, z.s.", "TJ B");
    }

    @Test
    void emptyRequestHasNoAdvancedRestrictionsAndLegacySeasonStillBinds() {
        StatisticsFilter filter = new StatisticsFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("seasonId", "12");
        new ServletRequestDataBinder(filter).bind(request);
        assertThat(filter.getSeasonId()).isEqualTo(12L);
        assertThat(filter.getSeasonIds()).isEmpty();
        assertThat(filter.getPlayerIds()).isEmpty();
        assertThat(filter.getFineIds()).isEmpty();
        assertThat(filter.getOpponentNames()).isEmpty();
    }
}
