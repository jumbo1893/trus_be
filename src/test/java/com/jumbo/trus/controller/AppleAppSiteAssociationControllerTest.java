package com.jumbo.trus.controller;

import com.jumbo.trus.config.ApplicationSecurityConfiguration;
import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationFilter;
import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppleAppSiteAssociationController.class)
@ContextConfiguration(classes = {
        AppleAppSiteAssociationController.class,
        ApplicationSecurityConfiguration.class,
        FirebaseAuthenticationFilter.class
})
class AppleAppSiteAssociationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuthenticationService firebaseAuthenticationService;

    @Test
    void returnsWebCredentialsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/.well-known/apple-app-site-association"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.webcredentials.apps[0]")
                        .value(AppleAppSiteAssociationController.APP_IDENTIFIER));
    }

    @Test
    void keepsOtherEndpointsProtected() throws Exception {
        mockMvc.perform(get("/not-a-public-endpoint"))
                .andExpect(status().isUnauthorized());
    }
}
