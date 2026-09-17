package com.jumbo.trus.repository.notification.push;

import com.jumbo.trus.entity.notification.NotificationEntity;
import com.jumbo.trus.entity.notification.push.log.SentPushNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SentPushNotificationRepository extends PagingAndSortingRepository<SentPushNotification, Long>, JpaRepository<SentPushNotification, Long>, JpaSpecificationExecutor<NotificationEntity> {

    // Transaction-scoped PostgreSQL lock, shared across replicas of the 09:00 greeting job.
    @org.springframework.data.jpa.repository.Query(value = "SELECT pg_try_advisory_xact_lock(903150901)", nativeQuery = true)
    boolean tryGreetingJobLock();

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(s) > 0 FROM sent_push_notification s
            WHERE s.deviceToken.user.id = :userId
              AND (s.deviceToken.token = :token OR
                   (:deviceId IS NOT NULL AND s.deviceToken.clientDeviceId = :deviceId))
              AND s.title = :title AND s.body = :body AND s.status = 'SENT'
              AND s.sentTime >= :from AND s.sentTime < :until
            """)
    boolean wasGreetingSent(@org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("token") String token,
            @org.springframework.data.repository.query.Param("deviceId") String deviceId,
            @org.springframework.data.repository.query.Param("title") String title,
            @org.springframework.data.repository.query.Param("body") String body,
            @org.springframework.data.repository.query.Param("from") java.util.Date from,
            @org.springframework.data.repository.query.Param("until") java.util.Date until);


}

