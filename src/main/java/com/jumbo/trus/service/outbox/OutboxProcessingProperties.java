package com.jumbo.trus.service.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "outbox.processing")
public class OutboxProcessingProperties {

    private int maxAttempts = 5;
    private Duration retryDelay = Duration.ofMinutes(1);
    private Duration staleTimeout = Duration.ofMinutes(10);
}
