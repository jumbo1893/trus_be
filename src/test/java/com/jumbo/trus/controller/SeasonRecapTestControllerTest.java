package com.jumbo.trus.controller;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.recap.SeasonRecapService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeasonRecapTestControllerTest {
    @Test void onlyAdminOfSelectedTeamCanTargetAnotherUser() throws Exception {
        for(String roleName:new String[]{"READER","EDITOR","ADMIN"}) {
            var service=mock(SeasonRecapService.class);var users=mock(UserService.class);var teams=mock(AppTeamService.class);
            var user=new UserEntity();user.setId(7L);when(users.getCurrentUserEntity()).thenReturn(user);
            var team=new AppTeamEntity();team.setId(5L);when(teams.getCurrentAppTeamOrThrow()).thenReturn(team);
            var role=new com.jumbo.trus.entity.auth.UserTeamRole();role.setAppTeam(team);role.setRole(roleName);user.getTeamRoles().add(role);
            when(service.testPublishLatest(8,5)).thenReturn(new SeasonRecapService.TestPublication(123,"Jaro",false,true));
            var mvc=MockMvcBuilders.standaloneSetup(new SeasonRecapTestController(service,users,teams)).build();
            mvc.perform(post("/season-recap/test/publish?userId=8"))
                    .andExpect(status().is(roleName.equals("ADMIN")?200:403));
            if(roleName.equals("ADMIN"))verify(service).testPublishLatest(8,5);
            else verifyNoInteractions(service);
            // Admin in another team is not enough.
            team.setId(6L);
            var selected=new AppTeamEntity();selected.setId(5L);when(teams.getCurrentAppTeamOrThrow()).thenReturn(selected);
            mvc.perform(post("/season-recap/test/publish?userId=8")).andExpect(status().isForbidden());
        }
    }
    @Test void testEndpointIsAbsentInProductionIncludingMixedProfiles() {
        for(var profiles:new String[][]{{"dev"},{"test"},{"prod"},{"production"},{"dev","prod"},{"test","production"},{}}) {
            try(var context=new AnnotationConfigApplicationContext()) {
                context.getEnvironment().setActiveProfiles(profiles);
                context.registerBean(SeasonRecapService.class,()->mock(SeasonRecapService.class));
                context.registerBean(UserService.class,()->mock(UserService.class));
                context.registerBean(AppTeamService.class,()->mock(AppTeamService.class));
                context.register(SeasonRecapTestController.class);context.refresh();
                boolean enabled=profiles.length==1&&(profiles[0].equals("dev")||profiles[0].equals("test"));
                assertThat(context.getBeansOfType(SeasonRecapTestController.class)).hasSize(enabled?1:0);
            }
        }
    }
    @Test void publishesForAuthenticatedUserAndSelectedTeamWithoutRequestBody() throws Exception {
        var service=mock(SeasonRecapService.class);var users=mock(UserService.class);var teams=mock(AppTeamService.class);
        var user=new UserEntity();user.setId(7L);when(users.getCurrentUserEntity()).thenReturn(user);
        var team=new AppTeamEntity();team.setId(5L);when(teams.getCurrentAppTeamOrThrow()).thenReturn(team);
        when(service.testPublishLatest(7,5)).thenReturn(new SeasonRecapService.TestPublication(123,"Jaro 2026",false,true));
        var mvc=MockMvcBuilders.standaloneSetup(new SeasonRecapTestController(service,users,teams)).build();
        mvc.perform(post("/season-recap/test/publish"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recapId").value(123))
                .andExpect(jsonPath("$.opened").value(false)).andExpect(jsonPath("$.pushScheduled").value(true));
        verify(service).testPublishLatest(7,5);
    }
}
