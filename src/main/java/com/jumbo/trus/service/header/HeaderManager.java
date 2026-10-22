package com.jumbo.trus.service.header;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
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

    public String getAppVersionHeader() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("x-app-version");
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
            log.debug("Client IP diagnostic: no HTTP request context");
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        String remoteAddr = request.getRemoteAddr();
        String clientIp = xff != null && !xff.isBlank()
                ? xff.split(",")[0].trim()
                : remoteAddr;
        log.info("Client IP diagnostic: xForwardedFor={}, remoteAddr={}, selectedIp={}",
                safeForLog(xff), safeForLog(remoteAddr), safeForLog(clientIp));
        return clientIp;
    }

    private String safeForLog(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n\\t]", "_");
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

}


