package com.jumbo.trus.config.security.firebase;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FirebaseAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void downstreamFailureIsNotConvertedToUnauthorized() throws Exception {
        var service = mock(FirebaseAuthenticationService.class);
        var authentication = mock(Authentication.class);
        when(service.authenticate("valid-token")).thenReturn(authentication);
        var filter = new FirebaseAuthenticationFilter(service);
        var request = request();
        var response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("home setup failed");
        };

        var failure = assertThrows(IllegalStateException.class,
                () -> filter.doFilter(request, response, chain));

        assertEquals("home setup failed", failure.getMessage());
        assertEquals(200, response.getStatus());
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void authenticationFailureStillReturnsUnauthorized() throws Exception {
        var service = mock(FirebaseAuthenticationService.class);
        when(service.authenticate("valid-token")).thenThrow(new IllegalArgumentException("bad token"));
        var filter = new FirebaseAuthenticationFilter(service);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request(), response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("{\"message\":\"Firebase token is invalid or expired\",\"code\":\"not_logged_in\"}",
                response.getContentAsString());
        verifyNoInteractions(chain);
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/home/setup");
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }
}
