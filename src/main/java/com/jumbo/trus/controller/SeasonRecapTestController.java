package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.recap.SeasonRecapService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** Manual end-to-end testing only. Never expose in production. */
@Profile("(dev | test) & !prod & !production")
@RestController
@RequestMapping(value = "/season-recap/test", produces = "application/json")
@RequiredArgsConstructor
public class SeasonRecapTestController {
    private final SeasonRecapService service;
    private final UserService users;
    private final AppTeamService teams;

    @PostMapping("/publish")
    @RoleRequired("READER")
    public SeasonRecapService.TestPublication publish(@RequestParam(required=false) Long userId) {
        var actor=users.getCurrentUserEntity();
        long teamId=teams.getCurrentAppTeamOrThrow().getId();
        long target=userId==null?actor.getId():userId;
        if(target<=0)throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,"userId musí být kladné ID uživatele.");
        if(target!=actor.getId() && actor.getTeamRoles().stream().noneMatch(role ->
                role.getAppTeam().getId()==teamId && "ADMIN".equalsIgnoreCase(role.getRole()))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,"Test pro jiného uživatele může spustit jen správce týmu.");
        }
        return service.testPublishLatest(target,teamId);
    }
}
