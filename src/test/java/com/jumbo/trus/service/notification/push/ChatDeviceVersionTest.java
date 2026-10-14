package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.auth.AuthService;
import com.jumbo.trus.service.header.HeaderManager;
import com.jumbo.trus.service.notification.settings.EnabledPushNotificationInitializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatDeviceVersionTest {
    @Test void registeringTokenStoresTheInstalledAppVersion() {
        DeviceTokenRepository repository = mock(DeviceTokenRepository.class);
        AuthService auth = mock(AuthService.class);
        HeaderManager headers = mock(HeaderManager.class);
        UserEntity user = new UserEntity(); user.setId(7L);
        when(auth.getCurrentUserEntity()).thenReturn(user);
        when(headers.getAppVersionHeader()).thenReturn("11.0.0");
        when(repository.findAllByToken("fcm-token")).thenReturn(List.of());
        when(repository.findByUser_IdAndClientDeviceIdAndStatus(7L, "device-1", "ACTIVE"))
                .thenReturn(List.of());
        when(repository.save(any(DeviceToken.class))).thenAnswer(call -> call.getArgument(0));
        DeviceTokenCollector collector = new DeviceTokenCollector(repository, auth, headers,
                mock(EnabledPushNotificationInitializer.class));
        DeviceTokenDTO request = new DeviceTokenDTO();
        request.setToken("fcm-token"); request.setClientDeviceId("device-1");

        DeviceTokenDTO saved = collector.addNewToken(request);

        assertEquals("11.0.0", saved.getAppVersion());
    }
}
