package com.jumbo.trus.controller;

import com.jumbo.trus.dto.home.HomeSetup;
import com.jumbo.trus.service.home.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    HomeService homeService;

    @GetMapping("/setup")
    public HomeSetup getHomeSetup(@RequestParam(required = false) Long playerId, @RequestParam(required = false) Boolean updateNeeded) {
        return homeService.setup(playerId, updateNeeded);
    }

}
