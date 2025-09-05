package com.jumbo.trus.repository.notification.push;

import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.notification.push.NotificationFootballMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationFootballMatchRepository  extends JpaRepository<NotificationFootballMatch, Long> {

    List<NotificationFootballMatch> findByFootballMatch(FootballMatchEntity footballMatch);

}

