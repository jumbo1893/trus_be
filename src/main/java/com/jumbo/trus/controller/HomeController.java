package com.jumbo.trus.controller;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.HomeSetup;
import com.jumbo.trus.service.FineService;
import com.jumbo.trus.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    HomeService homeService;


    @GetMapping("/setup")
    public HomeSetup getFines() {
        return homeService.setup();
    }

}
