package com.jumbo.trus.service.notification.settings;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.settings.EnabledPushNotification;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.repository.notification.push.EnabledPushNotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatPushDefaultTest {
    @SuppressWarnings("unchecked")
    @Test void newlyAddedChatPushSettingIsEnabled() {
        EnabledPushNotificationRepository repository = mock(EnabledPushNotificationRepository.class);
        UserEntity user = new UserEntity(); user.setId(7L);
        when(repository.findAllByUser(user)).thenReturn(List.of());

        new EnabledPushNotificationInitializer(repository, mock(DeviceTokenRepository.class))
                .ensureUserHasAllTypes(user);

        ArgumentCaptor<List<EnabledPushNotification>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertTrue(saved.getValue().stream()
                .filter(setting -> setting.getType() == NotificationType.CHAT)
                .findFirst().orElseThrow().getEnabled());
        assertTrue(saved.getValue().stream()
                .filter(setting -> setting.getType() == NotificationType.GLOBAL)
                .findFirst().orElseThrow().getEnabled());
    }
}
