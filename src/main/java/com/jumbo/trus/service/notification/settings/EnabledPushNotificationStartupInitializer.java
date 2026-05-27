package com.jumbo.trus.service.notification.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnabledPushNotificationStartupInitializer implements CommandLineRunner {

    private final EnabledPushNotificationInitializer enabledPushNotificationInitializer;

    @Override
    public void run(String... args) {
        enabledPushNotificationInitializer.ensureAllActiveTokenUsersHaveAllTypes();
    }
}