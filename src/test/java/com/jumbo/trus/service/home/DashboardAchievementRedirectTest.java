package com.jumbo.trus.service.home;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.helper.Redirect;
import com.jumbo.trus.dto.helper.TextWithRedirect;
import com.jumbo.trus.dto.home.stats.StatsBoardData;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.player.PlayerAchievementService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DashboardAchievementRedirectTest {
    @Test
    void bothDashboardSourcesLinkToTheExactEarnedRecord() {
        var api = mock(PlayerAchievementService.class);
        var home = mock(HomeService.class, CALLS_REAL_METHODS);
        var board = mock(StatsBoardDataService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(home, "playerAchievementService", api);
        ReflectionTestUtils.setField(board, "playerAchievementService", api);
        var definition = new AchievementDTO(); definition.setName("Střelec");
        var player = new PlayerDTO(); player.setId(42L); player.setName("Jan");
        var earned = new PlayerAchievementDTO(definition, player, true);
        earned.setId(123L); earned.setAccomplishedDate(new Date());
        var team = new AppTeamEntity(); team.setId(7L);
        var match = new MatchDTO(); match.setId(55L);
        when(api.getAllAccomplishedAchievementsByMatch(7L, 55L, null)).thenReturn(List.of(earned));
        when(api.getLastPlayerAchievements(5, List.of(42L))).thenReturn(List.of(earned));
        List<TextWithRedirect> warnings = ReflectionTestUtils.invokeMethod(home, "getAccomplishedAchievements", match, null, team);
        StatsBoardData stats = ReflectionTestUtils.invokeMethod(board, "getPlayerAchievementData", List.of(42L));
        assertThat(warnings).hasSize(1);
        assertThat(stats.getRows()).hasSize(1);
        for (var redirect : List.of(warnings.get(0).getRedirect(), stats.getRows().get(0).getRedirect())) {
            assertThat(redirect.getRedirect()).isEqualTo(Redirect.ACHIEVEMENTS);
            assertThat(redirect.getPlayerAchievement()).isSameAs(earned);
        }
    }
}
