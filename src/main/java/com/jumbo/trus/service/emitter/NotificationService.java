package com.jumbo.trus.service.emitter;

import com.jumbo.trus.dto.EventDTO;

public interface NotificationService {
    void sendNotification(String memberId, EventDTO event);
}
