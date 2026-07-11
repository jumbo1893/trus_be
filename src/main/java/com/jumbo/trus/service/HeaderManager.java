package com.jumbo.trus.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class HeaderManager {

    private final HttpServletRequest request;

    public HeaderManager(HttpServletRequest request) {
        this.request = request;
    }

    public Long getTeamIdHeader() {
        String teamId = request.getHeader("team-id");
        if (teamId == null) {
            return null;
        }
        return Long.parseLong(teamId);
    }

    public Long getAppTeamIdHeader() {
        String teamId = request.getHeader("app-team-id");
        if (teamId == null) {
            return null;
        }
        return Long.parseLong(teamId);
    }

    public String getDeviceHeader() {
        return request.getHeader("device");
    }

    public String getAnotherHeader() {
        return request.getHeader("Another-Header");
    }

    public String getClientIp() {
        String xff = request.getHeader("X-Forwarded-For");

        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}


