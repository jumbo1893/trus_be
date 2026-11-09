package com.jumbo.trus.service.auth;

import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.notification.push.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamMemberPendingPush {
    private final DeviceTokenRepository tokens;
    private final PushService push;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPendingMember(TeamMemberPendingEvent event) {
        if (event.appTeamId() == null || event.userTeamRoleId() == null) return;
        Map<String, String> data = Map.of(
                "screenId", "team-administration-screen",
                "appTeamId", event.appTeamId().toString(),
                "userTeamRoleId", event.userTeamRoleId().toString(),
                "notificationType", NotificationType.GLOBAL.name());
        for (var token : tokens.findActiveAdminTokensByAppTeamId(event.appTeamId())) {
            if (token.getUser().getId().equals(event.userId())) continue;
            try {
                push.sendPush(token, "Nový hráč v týmu",
                        event.userName() + " čeká na přidělení práv.",
                        NotificationType.GLOBAL, data);
            } catch (Exception exception) {
                log.warn("Nepodařilo se odeslat upozornění na nového člena týmu {}",
                        event.appTeamId(), exception);
            }
        }
    }
}
