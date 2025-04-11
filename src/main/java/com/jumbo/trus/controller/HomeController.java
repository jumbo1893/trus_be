package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.home.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final AppTeamService appTeamService;

    @RoleRequired("READER")
    @GetMapping("/setup")
    public HomeSetup getHomeSetup() {
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return homeService.setup(user.getId(), appTeamService.getCurrentAppTeamOrThrow());
    }

}
