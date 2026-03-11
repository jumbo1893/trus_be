package com.jumbo.trus.service.activity.footbar;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "footbar")
@Data
public class FootbarProperties {

    private String serverUrl;
    private String url;
    private String callbackEndpoint;
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String epSessionList;
    private String epSessionDetail;
    private String epProfileDetail;

    public String returnSessionListUrl() {
        return url+epSessionList;
    }

    public String returnSessionDetailUrl() {
        return url+epSessionDetail;
    }

    public String returnProfileDetailUrl() {
        return url+epProfileDetail;
    }
}
