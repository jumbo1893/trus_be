package com.jumbo.trus.entity.notification.push;

import com.jumbo.trus.entity.football.FootballMatchEntity;

public interface NotificationPair {
    DeviceToken getDeviceToken();
    FootballMatchEntity getFootballMatch();
}
