package com.jumbo.trus.repository.notification.push;

import com.jumbo.trus.entity.notification.NotificationEntity;
import com.jumbo.trus.entity.notification.push.SentPushNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SentPushNotificationRepository extends PagingAndSortingRepository<SentPushNotification, Long>, JpaRepository<SentPushNotification, Long>, JpaSpecificationExecutor<NotificationEntity> {


}

