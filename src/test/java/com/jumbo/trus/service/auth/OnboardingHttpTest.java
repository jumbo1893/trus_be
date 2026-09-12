package com.jumbo.trus.service.auth;

import com.jumbo.trus.config.JacksonConfig;
import com.jumbo.trus.controller.OnboardingController;
import com.jumbo.trus.controller.UserController;
import com.jumbo.trus.service.football.team.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OnboardingHttpTest {
    final OnboardingService service = mock(OnboardingService.class);
    final UserController legacy = new UserController(mock(UserService.class), mock(AppTeamService.class), mock(TeamService.class));
    MockMvc mvc(boolean includeOnboarding) {
        Object[] controllers = includeOnboarding ? new Object[]{legacy, new OnboardingController(service)} : new Object[]{legacy};
        return MockMvcBuilders.standaloneSetup(controllers).setMessageConverters(new MappingJackson2HttpMessageConverter(
                new JacksonConfig().objectMapper(new Jackson2ObjectMapperBuilder()))).build();
    }

    @Test void originalRequestIsValidWithCurrentBackend() throws Exception {
        when(service.advance(null)).thenReturn(new OnboardingService.State(true, false, Set.of()));
        mvc(true).perform(put("/user/onboarding").contentType(MediaType.APPLICATION_JSON).content("{\"completedStep\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
        verify(service).advance(null);
    }

    @Test void oldBackendTreatsOnboardingAsNumericUserIdAndReturns400() throws Exception {
        mvc(false).perform(put("/user/onboarding").contentType(MediaType.APPLICATION_JSON).content("{\"completedStep\":null}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void explicitStartRouteWorksAndCannotHitLegacyUserUpdate() throws Exception {
        when(service.advance(null)).thenReturn(new OnboardingService.State(true, false, Set.of()));
        mvc(true).perform(post("/user/onboarding/start")).andExpect(status().isOk()).andExpect(jsonPath("$.autoShow").value(false));
        mvc(false).perform(post("/user/onboarding/start")).andExpect(status().isNotFound());
    }

    @Test void completedStepEnumUsesRealJacksonConfiguration() throws Exception {
        when(service.advance(OnboardingService.Step.INTRO)).thenReturn(new OnboardingService.State(true, false, Set.of(OnboardingService.Step.INTRO)));
        mvc(true).perform(put("/user/onboarding").contentType(MediaType.APPLICATION_JSON).content("{\"completedStep\":\"INTRO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed[0]").value("INTRO"));
    }
}
