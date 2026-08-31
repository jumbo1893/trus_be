package com.jumbo.trus.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AppleAppSiteAssociationController {

    static final String APP_IDENTIFIER = "W2Y3H5JG6C.com.jumbo.trus";

    @GetMapping(
            value = "/.well-known/apple-app-site-association",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> getAppleAppSiteAssociation() {
        return Map.of(
                "webcredentials", Map.of(
                        "apps", List.of(APP_IDENTIFIER)
                )
        );
    }
}
