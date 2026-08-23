package com.jumbo.trus.service.header;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeaderManagerTest {

    private final HeaderManager headerManager = new HeaderManager();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsNullHeadersOutsideHttpRequest() {
        RequestContextHolder.resetRequestAttributes();

        assertNull(headerManager.getTeamIdHeader());
        assertNull(headerManager.getAppTeamIdHeader());
        assertNull(headerManager.getDeviceHeader());
        assertNull(headerManager.getOperationId());
        assertNull(headerManager.getClientIp());
    }

    @Test
    void readsHeadersFromCurrentHttpRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("team-id", "12");
        request.addHeader("app-team-id", "34");
        request.addHeader("device", "android");
        request.addHeader("X-Operation-Id", "01ae1e91-efc9-40e7-bb6d-4edb246e1874");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals(12L, headerManager.getTeamIdHeader());
        assertEquals(34L, headerManager.getAppTeamIdHeader());
        assertEquals("android", headerManager.getDeviceHeader());
        assertEquals("01ae1e91-efc9-40e7-bb6d-4edb246e1874", headerManager.getOperationId());
        assertEquals("203.0.113.10", headerManager.getClientIp());
    }

    @Test
    void operationContextGeneratesIdOutsideHttpRequest() {
        RequestContextHolder.resetRequestAttributes();
        OperationContext operationContext = new OperationContext(headerManager);

        UUID operationId = operationContext.getOperationId();

        assertNotNull(operationId);
    }
}
