package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.SentPushNotification;
import com.jumbo.trus.repository.notification.push.SentPushNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class PushService {

    private final FcmService fcmService;
    private final DeviceTokenCollector deviceTokenCollector;
    private final SentPushNotificationRepository sentPushNotificationRepository;

    public DeviceTokenDTO addNewToken(DeviceTokenDTO deviceTokenDTO) {
        return deviceTokenCollector.addNewToken(deviceTokenDTO);
    }

    public void sendPush(DeviceToken deviceToken, String title, String body) throws Exception {
        if (fcmService.sendPush(deviceToken, title, body)) {
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
}
