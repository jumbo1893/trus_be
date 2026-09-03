package com.jumbo.trus.controller;

import com.jumbo.trus.config.ApplicationSecurityConfiguration;
import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationFilter;
import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivacyPolicyController.class)
@ContextConfiguration(classes = {
        PrivacyPolicyController.class,
        ApplicationSecurityConfiguration.class,
        FirebaseAuthenticationFilter.class
})
class PrivacyPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuthenticationService firebaseAuthenticationService;

    @Test
    void returnsPrivacyPolicyWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Privacy Policy for Trusí appka")))
                .andExpect(content().string(containsString("Health and fitness data (Health Data)")))
                .andExpect(content().string(containsString("Health Connect")))
                .andExpect(content().string(containsString("background health-data access")))
                .andExpect(content().string(containsString("Delete your personal data")));
    }

    @Test
    void supportsHtmlAliasWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/privacy-policy.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
