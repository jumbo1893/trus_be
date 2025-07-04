package com.jumbo.trus.controller;

import com.jumbo.trus.dto.auth.AppTeamRegistration;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appteam")
public class AppTeamController {

    private final AppTeamService appTeamService;

    @PostMapping("/create")
    public UserDTO createNewAppTeam(@RequestBody AppTeamRegistration appTeamRegistration) {
        return appTeamService.registerAppTeam(appTeamRegistration);
    }

    @PostMapping("/add")
    public UserDTO addUserToAppTeam(@RequestBody Long appTeamId) {
        return appTeamService.addCurrentUserToAppTeam(appTeamId);
    }
}
