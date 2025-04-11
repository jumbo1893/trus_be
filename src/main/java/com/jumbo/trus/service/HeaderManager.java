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

    public String getAnotherHeader() {
        return request.getHeader("Another-Header");
    }
}


