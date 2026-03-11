package com.jumbo.trus.repository.notification.push;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.settings.EnabledPushNotification;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnabledPushNotificationRepository extends JpaRepository<EnabledPushNotification, Long> {

    Optional<EnabledPushNotification> findByUserAndType(UserEntity user, NotificationType type);

    List<EnabledPushNotification> findAllByUser(UserEntity user);

}

