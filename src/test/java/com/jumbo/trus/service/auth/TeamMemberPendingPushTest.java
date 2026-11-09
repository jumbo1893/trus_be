package com.jumbo.trus.service.auth;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.notification.push.PushService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamMemberPendingPushTest {
    @Test
    void sendsAdminDirectLinkToNewMember() throws Exception {
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        PushService push = mock(PushService.class);
        DeviceToken token = new DeviceToken();
        UserEntity admin = new UserEntity();
        admin.setId(1L);
        token.setUser(admin);
        when(tokens.findActiveAdminTokensByAppTeamId(12L)).thenReturn(List.of(token));

        new TeamMemberPendingPush(tokens, push).onPendingMember(
                new TeamMemberPendingEvent(12L, 40L, 2L, "Petr"));

        verify(push).sendPush(token, "Nový hráč v týmu", "Petr čeká na přidělení práv.",
                NotificationType.GLOBAL, Map.of(
                        "screenId", "team-administration-screen",
                        "appTeamId", "12",
                        "userTeamRoleId", "40",
                        "notificationType", "GLOBAL"));
    }
}
