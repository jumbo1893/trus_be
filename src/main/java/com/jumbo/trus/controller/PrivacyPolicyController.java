package com.jumbo.trus.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivacyPolicyController {

    private static final Resource PRIVACY_POLICY =
            new ClassPathResource("static/privacy-policy.html");

    @GetMapping(
            value = {"/privacy-policy", "/privacy-policy.html"},
            produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8"
    )
    public Resource getPrivacyPolicy() {
        return PRIVACY_POLICY;
    }
}
