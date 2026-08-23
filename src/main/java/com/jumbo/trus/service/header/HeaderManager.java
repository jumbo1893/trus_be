package com.jumbo.trus.service.header;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HeaderManager {

    public Long getTeamIdHeader() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String teamId = request.getHeader("team-id");
        if (teamId == null) {
            return null;
        }
        return Long.parseLong(teamId);
    }

    public Long getAppTeamIdHeader() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String teamId = request.getHeader("app-team-id");
        if (teamId == null) {
            return null;
        }
        return Long.parseLong(teamId);
    }

    public String getDeviceHeader() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader("device");
    }

    public String getOperationId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader("X-Operation-Id");
    }

    public String getClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");

        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

}


