package com.jumbo.trus.service;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class HeaderManager {

    private final HttpServletRequest request;

    public HeaderManager(HttpServletRequest request) {
        this.request = request;
    }

    public String getCustomHeader() {
        return request.getHeader("X-Custom-Header");
    }

    public String getAnotherHeader() {
        return request.getHeader("Another-Header");
    }
}


