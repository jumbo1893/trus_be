package com.jumbo.trus.repository.notification.push;

import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.NotificationPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface PushNotificationRepository extends JpaRepository<DeviceToken, Long> {

    @Query("""
    SELECT DISTINCT dt AS deviceToken, fm AS footballMatch
    FROM DeviceToken dt
    JOIN dt.user a
    JOIN UserTeamRole utr ON utr.user.id = a.id
    JOIN AppTeamEntity at ON at.id = utr.appTeam.id
    JOIN TeamEntity t ON t.id = at.team.id
    JOIN FootballMatchEntity fm
        ON (fm.homeTeam.id = t.id OR fm.awayTeam.id = t.id)
    WHERE fm.date BETWEEN :from AND :to
    AND dt.status = 'ACTIVE'
    """)
    List<NotificationPair> findNotificationPairs(
            @Param("from") Date from,
            @Param("to") Date to
    );
}
