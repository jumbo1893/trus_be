package com.jumbo.trus.service.activity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strava")
@Data
public class StravaProperties {

    private String url;
    private String callbackEndpoint;
    private String clientId;
    private String clientSecret;
}
