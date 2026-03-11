package com.jumbo.trus.service.notification.settings;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.settings.EnabledPushNotification;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
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

    @Transactional
    public void ensureUserHasAllTypes(UserEntity user) {
        // 1) Existující typy pro uživatele
        List<EnabledPushNotification> existing = enRepo.findAllByUser(user);

        // Pojištění starších NULL hodnot – nastav na true
        List<EnabledPushNotification> toFix = new ArrayList<>();
        for (EnabledPushNotification e : existing) {
            if (e.getEnabled() == null) {
                e.setEnabled(true);
                toFix.add(e);
            }
        }
        if (!toFix.isEmpty()) {
            enRepo.saveAll(toFix);
        }

        // 2) Dopočti chybějící typy
        EnumSet<NotificationType> have = existing.stream()
                .map(EnabledPushNotification::getType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NotificationType.class)));

        EnumSet<NotificationType> missing = EnumSet.allOf(NotificationType.class);
        missing.removeAll(have);

        if (missing.isEmpty()) return;

        // 3) Vlož chybějící (enabled = true)
        Date now = new Date();
        List<EnabledPushNotification> toInsert = missing.stream().map(t -> {
            EnabledPushNotification e = new EnabledPushNotification();
            e.setUser(user);
            e.setType(t);
            e.setEnabled(true);
            e.setModificationTime(now);
            return e;
        }).toList();

        enRepo.saveAll(toInsert);
    }
}
