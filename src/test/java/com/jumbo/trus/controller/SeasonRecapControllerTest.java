package com.jumbo.trus.controller;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.auth.*;
import com.jumbo.trus.service.recap.SeasonRecapService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeasonRecapControllerTest {
    @Test void returnsExplicitJsonAcknowledgementAndSupportsOwnerRegeneration() throws Exception {
        var service=mock(SeasonRecapService.class);var users=mock(UserService.class);
        var user=new UserEntity();user.setId(7L);when(users.getCurrentUserEntity()).thenReturn(user);
        var mvc=MockMvcBuilders.standaloneSetup(new SeasonRecapController(service,users,mock(AppTeamService.class))).build();
        mvc.perform(post("/season-recap/123/opened").contentType("application/json").content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.opened").value(true));
        verify(service).opened(123,7);
        when(service.regenerate(123,7,true)).thenReturn(new SeasonRecapService.Summary(123,"Podzim","2026-06-02","2026-12-01",false));
        mvc.perform(post("/season-recap/123/regenerate?resetOpened=true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.opened").value(false));
        verify(service).regenerate(123,7,true);
    }
}
