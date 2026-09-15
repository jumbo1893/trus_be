package com.jumbo.trus.service.notification.push.maker;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class AchievementNotificationMakerTest {
    @Test void singleAwardOpensSpecificAwardAndMultipleKeepProfile() {
        var award = new PlayerAchievementDTO(); award.setId(123L);
        var data = new HashMap<String, String>(); data.put("screenId", "view-player-screen");
        AchievementNotificationMaker.addAchievementTarget(data, List.of(award));
        assertThat(data).containsEntry("playerAchievementId", "123")
                .containsEntry("screenId", "view-player-achievement-detail-screen");
        var batch = new HashMap<String, String>(); batch.put("screenId", "view-player-screen");
        AchievementNotificationMaker.addAchievementTarget(batch, List.of(award, award));
        assertThat(batch).containsEntry("screenId", "view-player-screen").doesNotContainKey("playerAchievementId");
    }
}
