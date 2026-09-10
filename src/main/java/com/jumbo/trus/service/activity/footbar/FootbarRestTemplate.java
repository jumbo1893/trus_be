package com.jumbo.trus.service.activity.footbar;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Bound network waits so an unavailable Footbar cannot stall the job forever. */
@Component
public class FootbarRestTemplate extends RestTemplate {
    public FootbarRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        setRequestFactory(factory);
    }
}
