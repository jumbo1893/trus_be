package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.notification.push.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FootbarSyncNotifications {
    private final DeviceTokenRepository devices;
    private final PushService push;
    @org.springframework.beans.factory.annotation.Value("${notifications.enabled:true}")
    private boolean enabled = true;

    @Transactional
    public void notifyOwner(Long userId, String message) {
        if (!enabled) return;
        for (var device : devices.findByUser_IdIn(List.of(userId))) {
            if (!"ACTIVE".equals(device.getStatus())) continue;
            try {
                push.sendPush(device, "Zkontroluj propojení Footbaru", message, NotificationType.GLOBAL,
                        Map.of("screenId", "footbar-connect-screen", "navigateText", "Otevřít Footbar"));
            } catch (Exception e) {
                log.warn("Footbar warning push failed for user {}", userId, e);
            }
        }
    }
}
