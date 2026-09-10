package com.jumbo.trus.service.activity.footbar;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "footbar.auto-sync")
@org.springframework.validation.annotation.Validated
public class FootbarAutoSyncProperties {
    private boolean enabled = true;
    @jakarta.validation.constraints.Min(1)
    private int matchDurationMinutes = 60;
    @jakarta.validation.constraints.Min(1)
    private int windowMinutes = 60;
    @jakarta.validation.constraints.Min(1)
    private int retryMinutes = 5;
}
