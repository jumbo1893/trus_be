package com.jumbo.trus.service.notification.settings;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.settings.EnabledPushNotification;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.repository.notification.push.EnabledPushNotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnabledPushNotificationInitializer {

    private final EnabledPushNotificationRepository enRepo;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void ensureUserHasAllTypes(UserEntity user) {
        List<EnabledPushNotification> existing = enRepo.findAllByUser(user);

        List<EnabledPushNotification> toFix = new ArrayList<>();

        for (EnabledPushNotification notification : existing) {
            if (notification.getEnabled() == null) {
                notification.setEnabled(true);
                toFix.add(notification);
            }
        }

        if (!toFix.isEmpty()) {
            enRepo.saveAll(toFix);
        }

        EnumSet<NotificationType> existingTypes = existing.stream()
                .map(EnabledPushNotification::getType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NotificationType.class)));

        EnumSet<NotificationType> missingTypes = EnumSet.allOf(NotificationType.class);
        missingTypes.removeAll(existingTypes);

        if (missingTypes.isEmpty()) {
            return;
        }

        Date now = new Date();

        List<EnabledPushNotification> toInsert = missingTypes.stream()
                .map(type -> {
                    EnabledPushNotification notification = new EnabledPushNotification();
                    notification.setUser(user);
                    notification.setType(type);
                    notification.setEnabled(true);
                    notification.setModificationTime(now);
                    return notification;
                })
                .toList();

        enRepo.saveAll(toInsert);
    }

    @Transactional
    public void ensureAllActiveTokenUsersHaveAllTypes() {
        List<UserEntity> users = deviceTokenRepository.findDistinctActiveTokenUsers();

        for (UserEntity user : users) {
            ensureUserHasAllTypes(user);
        }
    }
}