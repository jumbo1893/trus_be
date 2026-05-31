package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.log.SentPushNotification;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.SentPushNotificationRepository;
import com.jumbo.trus.service.notification.settings.EnabledPushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushService {

    private final FcmService fcmService;
    private final DeviceTokenCollector deviceTokenCollector;
    private final SentPushNotificationRepository sentPushNotificationRepository;
    private final EnabledPushNotificationService enabledPushNotificationService;

    public DeviceTokenDTO addNewToken(DeviceTokenDTO deviceTokenDTO) {
        return deviceTokenCollector.addNewToken(deviceTokenDTO);
    }

    public void sendPush(DeviceToken deviceToken, String title, String body, NotificationType type) throws Exception {
        sendPush(deviceToken, title, body, type, Map.of());
    }

    public void sendPush(
            DeviceToken deviceToken,
            String title,
            String body,
            NotificationType type,
            Map<String, String> data
    ) throws Exception {
        if (enabledPushNotificationService.isNotificationEnabled(deviceToken.getUser(), type)
                && fcmService.sendPush(deviceToken, title, body, data)) {
            saveSentPushToRepository(title, body, deviceToken);
        }
    }

    private void saveSentPushToRepository(String title, String body, DeviceToken deviceToken) {
        SentPushNotification sentPushNotification = new SentPushNotification(
                deviceToken,
                title,
                body,
                new Date(),
                "SENT");
        sentPushNotificationRepository.save(sentPushNotification);
    }

    public void sendTestPushToCurrentDevice(DeviceTokenDTO deviceTokenDTO) throws Exception {
        DeviceTokenDTO savedTokenDto = addNewToken(deviceTokenDTO);

        DeviceToken deviceToken = deviceTokenCollector.findByIdOrThrow(savedTokenDto.getId());

        boolean sent = fcmService.sendPush(
                deviceToken,
                "Testovací pushka",
                "Pokud vidíš tuto zprávu, push notifikace fungují."
        );

        if (!sent) {
            throw new IllegalStateException("Testovací pushku se nepodařilo odeslat");
        }
    }
}
