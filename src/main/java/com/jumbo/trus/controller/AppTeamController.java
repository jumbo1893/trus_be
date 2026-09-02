package com.jumbo.trus.controller;

import com.jumbo.trus.dto.auth.AppTeamJoinRequest;
import com.jumbo.trus.dto.auth.AppTeamJoinResult;
import com.jumbo.trus.dto.auth.AppTeamRegistration;
import com.jumbo.trus.dto.auth.TeamAdministrationDTO;
import com.jumbo.trus.dto.auth.UpdateJoinCodeRequest;
import com.jumbo.trus.dto.auth.UpdateTeamMemberRoleRequest;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.TeamAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appteam")
public class AppTeamController {

    private final AppTeamService appTeamService;
    private final TeamAccessService teamAccessService;

    @PostMapping("/create")
    @RoleRequired("NONE")
    public UserDTO createNewAppTeam(@RequestBody AppTeamRegistration appTeamRegistration) {
        return appTeamService.registerAppTeam(appTeamRegistration);
    }

    @PostMapping("/add")
    @RoleRequired("NONE")
    @Deprecated
    public UserDTO addUserToAppTeam(@RequestBody Long appTeamId) {
        return appTeamService.addCurrentUserToAppTeam(appTeamId);
    }

    @PostMapping("/join-public")
    @RoleRequired("NONE")
    public UserDTO joinPublicAppTeam() {
        return appTeamService.addCurrentUserToPublicAppTeam();
    }

    @PostMapping("/join")
    @RoleRequired("NONE")
    public AppTeamJoinResult joinAppTeam(@RequestBody AppTeamJoinRequest request) {
        return teamAccessService.joinCurrentUser(request);
    }

    @GetMapping("/administration")
    @RoleRequired("ADMIN")
    public TeamAdministrationDTO getAdministration() {
        return teamAccessService.getAdministration();
    }

    @PutMapping("/administration/codes/{role}")
    @RoleRequired("ADMIN")
    public TeamAdministrationDTO updateJoinCode(
            @PathVariable String role,
            @RequestBody UpdateJoinCodeRequest request
    ) {
        return teamAccessService.updateJoinCode(role, request);
    }

    @PutMapping("/administration/members/{userTeamRoleId}/role")
    @RoleRequired("ADMIN")
    public TeamAdministrationDTO updateMemberRole(
            @PathVariable Long userTeamRoleId,
            @RequestBody UpdateTeamMemberRoleRequest request
    ) {
        return teamAccessService.updateMemberRole(userTeamRoleId, request);
    }
}
