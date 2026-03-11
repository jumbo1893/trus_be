package com.jumbo.trus.entity.notification.push.settings;

import lombok.Getter;

@Getter
public enum NotificationType {
    GLOBAL(0),
    THREE_DAYS_BEFORE(30),
    ONE_DAY_BEFORE(40),
    AFTER_RESULT(50),
    REFEREE_COMMENT(60),
    BEER(10),
    FINE(20);

    private final int order;
    NotificationType(int order) { this.order = order; }
}
