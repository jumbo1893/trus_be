package com.jumbo.trus.service.fine;

import com.jumbo.trus.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FineNotificationService {

    private final NotificationService notificationService;

    public void notifyFineAdded(String name, int amount) {
        notificationService.addNotification(
            "Přidána pokuta " + name, "ve výši " + amount + " Kč"
        );
    }

    public void notifyFineUpdated(String name, int amount) {
        notificationService.addNotification(
            "Upravena pokuta " + name, "ve výši " + amount + " Kč"
        );
    }

    public void notifyFineDeleted(String name, int amount) {
        notificationService.addNotification(
            "Smazána pokuta " + name, "ve výši " + amount + " Kč"
        );
    }
}
